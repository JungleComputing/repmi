package ibis.repmi.commTest;

/* $Id: Latency.java,v 1.15 2004/11/24 10:52:16 ceriel Exp $ */

import ibis.ipl.*;
import ibis.repmi.comm.*;
import ibis.repmi.protocol.LTVector;

import java.util.Properties;
import java.io.IOException;

class LatencyDummyMsg {

    /*
     * static Ibis ibis;
     * 
     * static Registry registry;
     * 
     * public static ReceivePortIdentifier lookup(String name) throws
     * IOException {
     * 
     * ReceivePortIdentifier temp = null;
     * 
     * do { temp = registry.lookupReceivePort(name);
     * 
     * if (temp == null) { try { Thread.sleep(1000); } catch (Exception e) { //
     * ignore } }
     *  } while (temp == null);
     * 
     * return temp; }
     * 
     * public static void main(String[] args) { Parse commandline.
     * 
     * boolean upcall = false; int NOPS = 1000;
     * 
     * if (args.length > 1) { upcall = args[1].equals("-u"); }
     * 
     * try { PoolInfo info = PoolInfo.createPoolInfo();
     * 
     * int rank = info.rank(); int size = info.size(); int remoteRank = (rank ==
     * 0 ? 1 : 0);
     * 
     * StaticProperties sp = new StaticProperties(); sp.add("serialization",
     * "object"); sp.add("communication", "OneToOne, OneToMany, Reliable,
     * ExplicitReceipt, Poll"); sp.add("worldmodel", "closed");
     * 
     * ibis = Ibis.createIbis(sp, null); registry = ibis.registry();
     * 
     * PortType t = ibis.createPortType("test type", sp);
     * 
     * ReceivePort rports[] = new ReceivePort[size-1]; SendPort sport =
     * t.createSendPort("send port " + rank); int tags[] = new int[size-1];
     * 
     * DummyVector ltv = new DummyVector(); DummyOperation op = new
     * DummyOperation();
     * 
     * for(int i=0,j=0; i<size-1; i++, j++) {
     * 
     * if(j == rank) j++; rports[i] = t.createReceivePort("receive port of " +
     * rank + " for " + j); rports[i].enableConnections(); tags[i] = 0; }
     * 
     * long start = System.currentTimeMillis(); long noBytes = 0;
     * 
     * System.setProperty("ibis.name_server.key","mpj_barrier"); MPJ.init(new
     * String[] {"mpj.localcopyIbis"}); MPJ.COMM_WORLD.barrier();
     * 
     * if (rank == 0) {
     * 
     * System.err.println(rank + "******* connecting to everyone");
     * 
     * for (int j=0; j<size; j++) {
     * 
     * if(j == rank) j++; ReceivePortIdentifier id = lookup("receive port of " +
     * j + " for " + rank);
     * 
     * System.err.println(rank + "******* connect to " + id);
     * 
     * sport.connect(id); }
     * 
     * System.err.println(rank + "******* connect done ");
     * 
     * MPJ.COMM_WORLD.barrier();
     * 
     * WriteMessage w; ReadMessage r;
     * 
     * 
     * 
     * for(int j = 0; j < NOPS; j++) { w = sport.newMessage(); w.writeObject(new
     * DummyMessage(ltv, op)); noBytes += w.finish();
     * 
     * 
     * System.err.println(rank + "******* receive acks");
     * 
     * int counter = 0;
     * 
     * while(counter < size-1) {
     * 
     * for(int k = 0; k < size-1; k++) { if(tags[k] == j) { r =
     * rports[k].poll(); if(r!=null) { counter ++; tags[k] ++; DummyMessage res =
     * (DummyMessage)r.readObject(); r.finish(); } } } } }
     * 
     * long end = System.currentTimeMillis();
     * 
     * System.err.println("Time per round: " + (double)(end-start)/NOPS);
     * System.err.println("Average noB/message: " + noBytes/NOPS);
     * 
     * sport.close();
     *  } else { System.err.println(rank + "******* connecting to everyone");
     * 
     * for (int j=0; j<size; j++) {
     * 
     * ReceivePortIdentifier id = lookup("receive port of " + j + " for " +
     * rank);
     * 
     * System.err.println(rank + "******* connect to " + id);
     * 
     * sport.connect(id);
     * 
     * if(j+1 == rank) j++; }
     * 
     * System.err.println(rank + "******* connect done ");
     * 
     * 
     * 
     * MPJ.COMM_WORLD.barrier();
     * 
     * ReadMessage r; WriteMessage w;
     * 
     * 
     * for(int j = 0; j < NOPS; j++) {
     * 
     * System.err.println(rank + "******* receive op");
     * 
     * r = rports[0].receive(); DummyMessage res = (DummyMessage)r.readObject();
     * tags[0] ++; r.finish();
     * 
     * w = sport.newMessage(); w.writeObject(new DummyMessage(ltv, op)); noBytes +=
     * w.finish();
     * 
     * System.err.println(rank + "******* receive acks");
     * 
     * int counter = 0;
     * 
     * while(counter < size-2) {
     * 
     * for(int k = 0; k < size-1; k++) { if(tags[k] == j) { r =
     * rports[k].poll(); if(r!=null) { counter ++; tags[k] ++; res =
     * (DummyMessage)r.readObject(); r.finish(); } } } } }
     * 
     * long end = System.currentTimeMillis();
     * 
     * System.err.println("Time per round: " + (double)(end-start)/NOPS);
     * System.err.println("Average noB/message: " + noBytes/NOPS);
     * 
     * sport.close(); }
     * 
     * MPJ.finish();
     * 
     * for(int i=0; i<size-1; i++) rports[i].close();
     * 
     * ibis.end();
     *  } catch (IOException e) { System.out.println("Got exception " + e);
     * System.out.println("StackTrace:"); e.printStackTrace(); } catch
     * (IbisException e) { System.out.println("Got exception " + e);
     * System.out.println("StackTrace:"); e.printStackTrace(); } catch
     * (MPJException mpje) { System.out.println("Got exception " + mpje);
     * System.out.println("StackTrace:"); mpje.printStackTrace(); } catch
     * (ClassNotFoundException e) { System.out.println("Got exception " + e);
     * System.out.println("StackTrace:"); e.printStackTrace(); } }
     */
}
