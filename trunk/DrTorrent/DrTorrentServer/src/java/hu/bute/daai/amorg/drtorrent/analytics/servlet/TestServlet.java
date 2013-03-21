/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.bute.daai.amorg.drtorrent.analytics.servlet;

import hu.bute.daai.amorg.drtorrent.analytics.entity.Process;
import hu.bute.daai.amorg.drtorrent.analytics.helper.DatabaseHelper;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Andreas
 */
public class TestServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        
        String s = request.getParameter("s");
        if (s == null || !s.equals("a8f3f3a8211db50be02b62eae6af696932c7607c")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "/DrTorrent/Test");
            return;
        }
        
        PrintWriter out = response.getWriter();
        
        DatabaseHelper db = null;
        try {
            db = new DatabaseHelper();
            
            ArrayList<Process> list = db.getProcesses();
            
            out.println("PeerIdentifier\tInfoHash\tAddedOn\tSize\tPieces\tBadPieces\tGoodPieces\tFailedConnections\tTcpConnections\tHandshakes");
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.JAPAN);
            for (Process item : list) {
                Date date = new Date(item.getAddedOn());
                String addedOn = dateFormat.format(date);
                
                out.println(item.getPeer().getPeerIdentifier() + "\t" +
                            item.getTorrent().getInfoHash() + "\t" +
                            addedOn + "\t" +
                            item.getTorrent().getSize() + "\t" +
                            item.getTorrent().getPieces() + "\t" +
                            item.getBadPieces() + "\t" +
                            item.getGoodPieces() + "\t" +
                            item.getFailedConnections() + "\t" +
                            item.getTcpConnections() + "\t" +
                            item.getHandshakes());
            }
        } catch (Exception e) {
            out.println("Exception: " + e.getMessage());
        } finally {         
            out.close();
            if (db != null) {
                db.close();
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
