/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.bute.daai.amorg.drtorrent.analytics.servlet;

import hu.bute.daai.amorg.drtorrent.analytics.entity.Peer;
import hu.bute.daai.amorg.drtorrent.analytics.entity.Torrent;
import hu.bute.daai.amorg.drtorrent.analytics.helper.DatabaseHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Andrew
 */
public class AnalyticsV3Servlet extends HttpServlet {

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        BufferedReader reader = request.getReader();
        String line = null;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        DatabaseHelper db = null;
        try {
            JSONObject json = new JSONObject(builder.toString());
            String peerIdentifier = json.getString("clientIdentifier").toLowerCase();
            JSONArray torrents = json.getJSONArray("torrents");
            JSONArray networkInfo = json.getJSONArray("networkInfo");
            JSONArray powerInfo = json.getJSONArray("powerInfo");
            
            db = new DatabaseHelper();
            Peer peer = db.addPeer(peerIdentifier);
            
            for (int i = 0; i < torrents.length(); i++) {
                json = torrents.getJSONObject(i);
                String infoHash = json.getString("infoHash").toUpperCase();
                long addedOn = json.getLong("addedOn");
                long size = json.getLong("size");
                int pieces = json.getInt("pieces");
                
                int badPieces = json.getInt("badPieces");
                int goodPieces = json.getInt("goodPieces");
                int failedConnections = json.getInt("failedConnections");
                int tcpConnections = json.getInt("tcpConnections");
                int handshakes = json.getInt("handshakes");
                
                long downloaded = json.getLong("downloaded");
                long completed = json.getLong("completed");
                long uploaded = json.getLong("uploaded");
                long downloadingTime = json.getLong("downloadingTime");
                long seedingTime = json.getLong("seedingTime");
                long completedOn = json.getLong("completedOn");
                long removedOn = json.getLong("removedOn");
                
                Torrent torrent = db.addTorrent(infoHash, size, pieces);
                
                db.saveOrUpdateProcess(peer, torrent, addedOn, badPieces, goodPieces, failedConnections, tcpConnections, handshakes, downloaded, completed, uploaded, downloadingTime, seedingTime, completedOn, removedOn);
            }
            
            for (int i = 0; i < networkInfo.length(); i++) {
                json = networkInfo.getJSONObject(i);
                long from = json.getLong("from");
                long to = json.getLong("to");
                int type = json.getInt("type");
                
                db.saveOrUpdateNetworkInfo(peer, from, to, type);
            }
            
            for (int i = 0; i < powerInfo.length(); i++) {
                json = powerInfo.getJSONObject(i);
                long from = json.getLong("from");
                long to = json.getLong("to");
                boolean isPlugged = json.getBoolean("isPlugged");
                
                db.saveOrUpdatePowerInfo(peer, from, to, isPlugged);
            }
            
            out.print("200");
        } catch (Exception e) {
            out.print("400");
        } finally {
            out.close();
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "AnalyticsV3 Servlet";
    }// </editor-fold>
}
