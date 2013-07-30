package mods.eln.elnhttpserver;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map.Entry;

import mods.eln.Eln;
import mods.eln.battery.BatteryElement;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeElement;
import mods.eln.sim.IProcess;
import mods.eln.sim.Simulator;
/*
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
*/

public class ElnHttpServer {
	public ElnHttpServer() throws Exception {
        /*HttpServer server = HttpServer.create(new InetSocketAddress(3068), 0);
        server.createContext("/battery", new BatteryHandler());
        server.createContext("/", new RootHandler());
        server.setExecutor(null); // creates a default executor
        server.start();*/
    }
  /*  static class RootHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String response = "";
            response += "<HTML>";
            response += "<head><meta http-equiv=\"refresh\" content=\"1\"></head>";           
            response += "<body><br>";
            response += "<b>Root</b> :<br>";
            response += "<A href=\"battery\">battery</A>";
        	response += "</body>";
        	response += "</HTML>";
           
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }*//*
    static class BatteryHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String response = "";
            response += "<HTML>";
            response += "<head><meta http-equiv=\"refresh\" content=\"1\"></head>";           
            response += "<body><br>";
            response += "<b>Battery</b> :<br>";
            
        	for(Entry<Coordonate,Node> entry : NodeManager.instance.getNodeArray().entrySet()){
        		Node node = entry.getValue();
        		if(node instanceof TransparentNode){
        			TransparentNode transparentNode = ((TransparentNode)node); 
        			TransparentNodeElement element = transparentNode.element;
        			if(element instanceof BatteryElement){
        				BatteryElement battery = (BatteryElement)element;
        				double energyMax = battery.batteryProcess.getEnergyMax();
        				double energy = battery.batteryProcess.getEnergy();
        				response += "  at " + entry.getKey() + Utils.plotEnergy("  Energy : ",energy) + Utils.plotPercent("Charge level : ",energy / energyMax);  
        				
        				response += "<br>";
        			}
        		}
        	}
        	
        	response += "</body>";
        	response += "</HTML>";
           
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }*/

}