/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.bute.daai.amorg.drtorrent.tracker.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Andreas
 */
public class AnnounceServlet extends HttpServlet {

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
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        try {
            String infoHash = request.getParameter("info_hash");
            //String compact = request.getParameter("compact");
            //String test = request.getParameter("test");
            
            if (infoHash == null) {
                out.println("d14:failure reason17:missing info hashe");
                return;
            }
            
            if (infoHash.equals("Correct")) {
                out.println("d8:intervali1800e8:completei23e10:incompletei7e5:peersld7:peer id20:abcdefghijklmnopqrst2:ip13:192.168.0.1004:porti6881eed7:peer id20:zyxwvutsrqponmlkjihi2:ip15:192.168.100.1284:porti6882eed7:peer id20:abc123efg456ijk7890z2:ip7:0.0.0.04:porti6883eeee");
            } else if (infoHash.equals("CorrectCompact")) {
                String str = "d8:intervali1900e8:completei37e10:incompletei13e5:peers";

                // 192.168.0.100:6886
                // 192.168.100.128:6887
                char [] ccc = new char[]{
                    192, 168, 0, 100, 26, 230,
                    192, 168, 100, 128, 26, 231,
                    0, 0, 0, 0, 0, 0
                };
                str += ccc.length + ":" + new String(ccc) + "e"; 

                out.println(str);
            } else {
                out.print("d14:failure reason17:info hash not founde");
            }
            
            
            
            /*// http://localhost:8084/DrTorrent/announce?info_hash=%01%23Eg%89%AB%cd%ef%01%23%45%67%89%ab%cd%ef%01%23%45%67
            byte [] c = {
                1, 35, 69, 103, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, 1, 35, 69, 103
            };
            
            byte[] b = infoHash.getBytes(Charset.forName("UTF-8"));
            
            boolean isEqual = true;
            for (int i = 0; i < c.length; i++) {
                if (test != null && test.equals("1")) {
                    out.print(b[i] + ", " + c[i] + ". ");
                }
                
                if (b[i] != c[i]) {
                    isEqual = false;
                }
            }
            
            if (isEqual) {
                if (compact.equals("0")) {
                    out.println("d8:intervali1800e8:completei23e10:incompletei7e5:peersld7:peer id20:abcdefghijklmnopqrst2:ip13:192.168.0.1004:porti6881eed7:peer id20:zyxwvutsrqponmlkjihi2:ip15:192.168.100.1284:porti6882eed7:peer id20:abc123efg456ijk7890z2:ip7:0.0.0.04:porti6883eeee");
                } else {
                    String str = "d8:intervali1800e8:completei23e10:incompletei7e5:peers";
                    
                    char [] ccc = new char[]{
                        13, 192, 168, 0, 100, 68, 81,
                        192, 168, 100, 128, 68, 82,
                        0, 0, 0, 0, 0, 0
                    };
                    str += ccc.length + ":" + new String(ccc) + "e"; 

                    out.println(str);
                }
                
            } else {
                out.print("d14:failure reason17:info hash not founde");
            }*/
        } finally {            
            out.close();
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
