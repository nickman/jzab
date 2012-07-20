import java.nio.*;
import groovy.json.*;
import java.util.concurrent.*;


def ss = null;
println "Opening Socket";
def requestSocket = new Socket("10.230.12.145", 10051);
println "Connected:${requestSocket}";
def initReq = new ByteArrayOutputStream();
initReq.write("ZBXD".getBytes());
initReq.write(1);

//def AREQ = "ZBX_GET_ACTIVE_CHECKS\nNE-WK-NWHI-01 Active\n".getBytes();
def AREQ = """{
   "request":"active checks",
   "host":"NicholasActive"
}""".getBytes();



initReq.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(AREQ.length).array());


initReq.write(AREQ);
def rqso = requestSocket.getOutputStream();
println "Sending ${initReq.toByteArray().length} Bytes";
rqso.write(initReq.toByteArray());
rqso.flush();


def rqsi = requestSocket.getInputStream();
def resp = new ByteArrayOutputStream();
buff = new byte[128];
int bytesRead = -1;
long total = 0;
for(i in 0..4) {  rqsi.read(); }
byte[] dl = new byte[8];
println "Reading DL:${rqsi.read(dl)}";
long dataLength = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put(dl).flip().getLong();
println "Data Length:${dataLength}";
dl = new byte[(int)dataLength];
rqsi.read(dl);


println "Read ${dl.length} bytes";

//println "Raw Response:${resp.toByteArray()}";
//responses = new String(dl).split("\n");
//println "Response:[${new String(resp.toByteArray())}]";
requestSocket.close();
//responses.each() { println "\t${it}"; }
//println new String(dl);
def actives = new JsonSlurper().parseText( new String(dl));
println "Response:${actives.response}";
int responseCnt = 0;
sb = new StringBuilder("""{
   "request":"sender data",
   "data":[
""");
int prg = 0;
int sleepTime = 0;
//"clock":${(int)(System.currentTimeMillis()/1000)}
actives.data.each() {
    prg++;
    sleepTime = it.delay;
    if(prg > 46) {
        sb.append("""{
               "host":"NicholasActive",
               "key":"${it.key.replace("\"", "\\\"")}",
               "value":1.0               
               },"""
        );
        responseCnt++;
    }
}
sb.deleteCharAt(sb.length()-1);
//sb.append("""], "clock":${(int)(System.currentTimeMillis()/1000)} }""");
sb.append("""]}""");

println sb.toString().replace("  ", "");
results = sb.toString().replace("  ", "").getBytes();
//println json;
println "\n\tSleeping for [${sleepTime}] secconds\n";
Thread.sleep(sleepTime*100);
println "Wake Up";
requestSocket = new Socket("10.230.12.145", 10051);
initReq = new ByteArrayOutputStream();
initReq.write("ZBXD".getBytes());
initReq.write(1);
initReq.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(results.length).array());
initReq.write(results);
rqso = requestSocket.getOutputStream();
println "Sending ${initReq.toByteArray().length} Bytes for ${responseCnt}  Responses";
rqso.write(initReq.toByteArray());
rqso.flush();
rqsi = requestSocket.getInputStream();
for(i in 0..4) {  rqsi.read(); }
dl = new byte[8];
println "Reading DL:${rqsi.read(dl)}";
dataLength = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put(dl).flip().getLong();
println "Data Length:${dataLength}";
dl = new byte[(int)dataLength];
rqsi.read(dl);
print new String(dl);
requestSocket.close();
return null;
/*
try {
    ss = new ServerSocket(10050, 100, InetAddress.getByName("192.168.56.1"));
    println ss;
    while(true) {
        def s = ss.accept();
        println "Accepted Connection from [${s.getRemoteSocketAddress()}]";
        Thread.startDaemon() {
            s.getInputStream().write(1);
            s.close();
            println "Done";
        }
    }
} finally {
    try { ss.close(); } catch (e) {}
}
*/