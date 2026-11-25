import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Main {
    static final BigDecimal ZERO = new BigDecimal("0.00"), DEPOSIT = new BigDecimal("2000.00"), GST = new BigDecimal("0.18");
    static class Pack { String id, location; int soc; LocalDateTime issued; Pack(String i,String l,String s,String t){id=i;location=l;soc=s.equals("NONE")?-1:Integer.parseInt(s);issued=t.equals("NONE")?null:LocalDateTime.parse(t.replace(" ","T"));} }
    static class Rider { String id,status="ACTIVE"; BigDecimal wallet; Set<String> held=new HashSet<>(); int swaps,declines,creditsForfeited; BigDecimal damage=ZERO,overdue=ZERO,forfeited=ZERO; Rider(String i,String w){id=i;wallet=money(w);} }
    static class Credit { String id,rider; LocalDate expiry; int remaining; Credit(String i,String r,String d,String n){id=i;rider=r;expiry=LocalDate.parse(d).plusDays(30);remaining=Integer.parseInt(n);} }
    static class Tx { String[] c; LocalDateTime time; Tx(String[] x){c=x;time=LocalDateTime.parse(x[1].replace(" ","T"));} }
    static class Upd { String[] c; LocalDateTime time; Upd(String[] x){c=x;time=LocalDateTime.parse(x[1].replace(" ","T"));} }
    static Map<String,Pack> packs=new LinkedHashMap<>(); static Map<String,Rider> riders=new TreeMap<>(); static Map<String,String> stations=new TreeMap<>(); static List<Credit> credits=new ArrayList<>(); static List<Tx> txs=new ArrayList<>(); static List<Upd> updates=new ArrayList<>();
    static List<String[]> log=new ArrayList<>(); static Map<String,long[]> daily=new TreeMap<>(); static Map<String,BigDecimal> revenue=new TreeMap<>(); static Map<String,Integer> redeemed=new TreeMap<>(); static Map<String,int[]> snapshots=new HashMap<>();
    static BigDecimal money(String s){return new BigDecimal(s).setScale(2,RoundingMode.HALF_UP);} static String fmt(BigDecimal x){return x.setScale(2,RoundingMode.HALF_UP).toPlainString();}
    static String[] read(String file)throws Exception{return Files.readString(Path.of(file)).split("\\R");}
    static List<String[]> csv(String file)throws Exception{List<String[]> out=new ArrayList<>();String[] lines=read(file);for(int i=1;i<lines.length;i++){String line=lines[i].trim();if(!line.isEmpty())out.add(line.split(",",-1));}return out;}
    static void load(String file,String type)throws Exception{for(String[] a:csv(file)){switch(type){case"stations":stations.put(a[0],a[1]);break;case"packs":packs.put(a[0],new Pack(a[0],a[1],a[2],a[3]));break;case"riders":riders.put(a[0],new Rider(a[0],a[4]));break;case"deposits":riders.get(a[0]).held.add(a[1]);break;case"credits":credits.add(new Credit(a[0],a[1],a[2],a[3]));break;case"tx":txs.add(new Tx(a));break;case"updates":updates.add(new Upd(a));break;}}}
    static String key(String date,String station){return date+"|"+station;}
    static void inc(String date,String station,int index){long[] a=daily.computeIfAbsent(key(date,station),k->new long[4]);a[index]++;}
    static BigDecimal deposit(String rider,String pack){ // opening deposit defaults to 2000 for a newly issued pack
        return deposits.getOrDefault(rider+"|"+pack,DEPOSIT);
    }
    static Map<String,BigDecimal> deposits=new HashMap<>();
    static int validCredits(String rider,LocalDate date){int n=0;for(Credit c:credits)if(c.rider.equals(rider)&&c.remaining>0&&!date.isAfter(c.expiry))n+=c.remaining;return n;}
    static int useCredit(String rider,LocalDate date){credits.sort(Comparator.comparing(c->c.expiry));for(Credit c:credits)if(c.rider.equals(rider)&&c.remaining>0&&!date.isAfter(c.expiry)){c.remaining--;return 1;}return 0;}
    static void expireCredits(String rider,LocalDate date){for(Credit c:credits)if(c.rider.equals(rider)&&c.remaining>0&&date.isAfter(c.expiry)){riders.get(rider).creditsForfeited+=c.remaining;c.remaining=0;}}
    static void applyUpdate(Upd u){String[] a=u.c;Pack p=packs.get(a[3]);if(p!=null){p.location=a[2];p.soc=Integer.parseInt(a[4]);}}
    static void process(Tx t){String[] a=t.c;String id=a[0],date=a[1].substring(0,10),type=a[2],rid=a[3],sid=a[4],ret=a[5],soc=a[6],grade=a[7],issue=a[8];Rider r=riders.get(rid);LocalDate day=LocalDate.parse(date);expireCredits(rid,day);
        String status="OK",reason="NONE",paid="NONE";BigDecimal price=ZERO,swapGst=ZERO,damage=ZERO,damageGst=ZERO,overdue=ZERO,forfeit=ZERO,released=ZERO;
        if(type.equals("CREDIT_PURCHASE")){credits.add(new Credit("NEW-"+id,rid,date,"30"));}
        else if(type.equals("RETURN")){ // explicit return only
            if(!r.held.contains(ret)){status="DECLINED";reason="NOT_HELD";}else{Pack p=packs.get(ret);BigDecimal dep=deposit(rid,ret);int days=(int)ChronoUnit.DAYS.between(p.issued.toLocalDate(),day);if(days>7)overdue=money(""+Math.min(dep.doubleValue(),(days-7)*50.0));BigDecimal left=dep.subtract(overdue);r.overdue=r.overdue.add(overdue);r.held.remove(ret);p.location=sid;p.soc=soc.equals("NONE")?0:Integer.parseInt(soc);p.issued=null;if(grade.equals("C")){forfeit=left;r.status="SUSPENDED";r.forfeited=r.forfeited.add(forfeit);}else{released=left;r.wallet=r.wallet.add(released);}deposits.remove(rid+"|"+ret);}}
        else { // SWAP or ISSUE
            boolean hasReturn=!ret.equals("NONE");
            if(hasReturn&&!r.held.contains(ret)){status="DECLINED";reason="NOT_HELD";}
            else if(r.status.equals("SUSPENDED")){status="DECLINED";reason="SUSPENDED";}
            else if(!hasReturn&&r.held.size()>=2){status="DECLINED";reason="PACK_LIMIT";}
            else if(!grade.equals("C") && (issue.equals("NONE")||!packs.containsKey(issue)||!packs.get(issue).location.equals(sid)||packs.get(issue).soc<90)){status="DECLINED";reason="NO_CHARGED_PACK";}
            else {
                BigDecimal depLeft=ZERO; if(hasReturn){Pack old=packs.get(ret);BigDecimal dep=deposit(rid,ret);int days=(int)ChronoUnit.DAYS.between(old.issued.toLocalDate(),day);if(days>7)overdue=money(""+Math.min(dep.doubleValue(),(days-7)*50.0));depLeft=dep.subtract(overdue);}
                if(hasReturn&&grade.equals("C")){ // Grade C is a return, never a swap
                    Pack old=packs.get(ret);r.held.remove(ret);old.location=sid;old.soc=soc.equals("NONE")?0:Integer.parseInt(soc);old.issued=null;forfeit=depLeft;r.forfeited=r.forfeited.add(forfeit);r.overdue=r.overdue.add(overdue);deposits.remove(rid+"|"+ret);r.status="SUSPENDED";
                } else {
                    price=money("150.00").subtract(money(soc).multiply(new BigDecimal("1.20"))).max(new BigDecimal("40.00")).setScale(2,RoundingMode.HALF_UP);
                    if(grade.equals("B")){damage=new BigDecimal("200.00");damageGst=damage.multiply(GST).setScale(2,RoundingMode.HALF_UP);}
                    boolean credit=useCredit(rid,day)==1;
                    BigDecimal total=price.add(price.multiply(GST).setScale(2,RoundingMode.HALF_UP)).add(damage).add(damageGst);
                    if(!credit&&r.wallet.compareTo(total)<0){status="DECLINED";reason="INSUFFICIENT_BALANCE";price=ZERO;damage=ZERO;damageGst=ZERO;}
                    else {if(credit){paid="CREDIT";price=price;swapGst=ZERO;redeemed.merge(key(date,sid),1,Integer::sum);}else{paid="WALLET";swapGst=price.multiply(GST).setScale(2,RoundingMode.HALF_UP);r.wallet=r.wallet.subtract(total);revenue.merge(key(date,sid),total,BigDecimal::add);}if(hasReturn){Pack old=packs.get(ret);r.held.remove(ret);old.location=sid;old.soc=Integer.parseInt(soc);old.issued=null;r.overdue=r.overdue.add(overdue);deposits.remove(rid+"|"+ret);}if(!issue.equals("NONE")){Pack np=packs.get(issue);np.location=rid;np.soc=-1;np.issued=t.time;r.held.add(issue);deposits.put(rid+"|"+issue,depLeft.signum()>0?depLeft:DEPOSIT);}r.swaps++;}
                }
            }
        }
        if(status.equals("DECLINED")){r.declines++;inc(date,sid,1);}else if(type.equals("SWAP")){inc(date,sid,0);}if(status.equals("OK")&&grade.equals("B")){r.damage=r.damage.add(damage);}if(!sid.equals("NONE")&&stations.containsKey(sid)&&status.equals("OK")){}
        log.add(new String[]{id,rid,sid,type,status,reason,fmt(price),paid,fmt(swapGst),fmt(damage),fmt(damageGst),fmt(overdue),fmt(forfeit),fmt(released),fmt(r.wallet),""+validCredits(rid,day)});
    }
    static void write(String file,List<String[]> rows)throws Exception{try(BufferedWriter w=Files.newBufferedWriter(Path.of(file))){for(String[] row:rows){w.write(String.join(",",row));w.newLine();}}}
    static void append(String file,List<String[]> rows)throws Exception{boolean exists=Files.exists(Path.of(file));try(BufferedWriter w=Files.newBufferedWriter(Path.of(file),StandardOpenOption.CREATE,StandardOpenOption.APPEND)){for(int i=0;i<rows.size();i++){if(exists&&i==0)continue;w.write(String.join(",",rows.get(i)));w.newLine();}}}
    static void run()throws Exception{List<Object> events=new ArrayList<>();events.addAll(updates);events.addAll(txs);events.sort((x,y)->{LocalDateTime a=x instanceof Upd?((Upd)x).time:((Tx)x).time,b=y instanceof Upd?((Upd)y).time:((Tx)y).time;int c=a.compareTo(b);if(c!=0)return c;return (x instanceof Upd?-1:1);});for(int i=0;i<events.size();){Object first=events.get(i);LocalDate eventDay=(first instanceof Upd?((Upd)first).time:((Tx)first).time).toLocalDate();while(i<events.size()){Object e=events.get(i);LocalDate d=(e instanceof Upd?((Upd)e).time:((Tx)e).time).toLocalDate();if(!d.equals(eventDay))break;if(e instanceof Upd)applyUpdate((Upd)e);else process((Tx)e);i++;}for(String sid:stations.keySet()){int at=0,ready=0;for(Pack p:packs.values())if(p.location.equals(sid)){at++;if(p.soc>=90)ready++;}snapshots.put(key(eventDay.toString(),sid),new int[]{at,ready});}}
        List<String[]> out=new ArrayList<>();out.add(new String[]{"txn_id","rider_id","station_id","txn_type","status","reason","swap_price_rs","paid_by","swap_gst_rs","damage_charge_rs","damage_gst_rs","overdue_charge_rs","deposit_forfeited_rs","deposit_released_rs","wallet_after_rs","credits_after"});out.addAll(log);write("swap_log.csv",out);
    }
    static void stationReport(String date)throws Exception{List<String[]> out=new ArrayList<>();out.add(new String[]{"date","station_id","swaps_ok","declines","wallet_revenue_rs","credits_redeemed","packs_at_station_end","packs_ready_end"});for(String sid:stations.keySet()){long[] d=daily.getOrDefault(key(date,sid),new long[4]);int[] snap=snapshots.getOrDefault(key(date,sid),new int[]{0,0});out.add(new String[]{date,sid,""+d[0],""+d[1],fmt(revenue.getOrDefault(key(date,sid),ZERO)),""+redeemed.getOrDefault(key(date,sid),0),""+snap[0],""+snap[1]});}append("station_daily.csv",out);}
    static void riderStatement(String date)throws Exception{LocalDate statementDay=LocalDate.parse(date);for(Rider rr:riders.values())expireCredits(rr.id,statementDay);List<String[]> out=new ArrayList<>();out.add(new String[]{"rider_id","status","wallet_rs","packs_held","deposit_held_rs","credits_valid","credits_forfeited","swaps_ok","declines","damage_charges_rs","overdue_charges_rs","deposits_forfeited_rs"});LocalDate day=LocalDate.parse(date);for(Rider r:riders.values()){BigDecimal dep=ZERO;for(String p:r.held)dep=dep.add(deposit(r.id,p));out.add(new String[]{r.id,r.status,fmt(r.wallet),""+r.held.size(),fmt(dep),""+validCredits(r.id,day),""+r.creditsForfeited,""+r.swaps,""+r.declines,fmt(r.damage),fmt(r.overdue),fmt(r.forfeited)});}write("rider_statement.csv",out);}
    public static void main(String[] args)throws Exception{Files.deleteIfExists(Path.of("station_daily.csv"));Files.deleteIfExists(Path.of("swap_log.csv"));Files.deleteIfExists(Path.of("rider_statement.csv"));Path base=Files.exists(Path.of("data/commands.txt"))?Path.of("data"):Path.of(".");for(String line:Files.readAllLines(base.resolve("commands.txt"))){line=line.trim();if(line.isEmpty()||line.startsWith("#"))continue;String[] c=line.split("\\|");switch(c[0]){case"LOAD_STATIONS":load(base.resolve(c[1]).toString(),"stations");break;case"LOAD_PACKS":load(base.resolve(c[1]).toString(),"packs");break;case"LOAD_RIDERS":load(base.resolve(c[1]).toString(),"riders");break;case"LOAD_DEPOSITS":for(String[] a:csv(base.resolve(c[1]).toString())){deposits.put(a[0]+"|"+a[1],money(a[2]));riders.get(a[0]).held.add(a[1]);}break;case"LOAD_CREDIT_PACKS":load(base.resolve(c[1]).toString(),"credits");break;case"LOAD_TRANSACTIONS":load(base.resolve(c[1]).toString(),"tx");break;case"LOAD_INVENTORY_UPDATES":load(base.resolve(c[1]).toString(),"updates");break;case"RUN":run();break;case"STATION_REPORT":stationReport(c[1]);break;case"RIDER_STATEMENT":riderStatement(c[1]);break;}}System.out.println("Engine completed. Output files: swap_log.csv, station_daily.csv, rider_statement.csv");}
}
