package app;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@WebServlet("/Servlet_Main")
public class Servlet_Main extends HttpServlet {
	private static final long serialVersionUID = 1L;
    String URL = "jdbc:mysql://localhost:3307/";
    String databaza = "chat";
    String userName = "root";
    String pass = "";
    Connection con;
    
    
    @Override
    public void init() throws ServletException {
     super.init();
     try {
    	 Class.forName("com.mysql.cj.jdbc.Driver");
         con = DriverManager.getConnection(URL + databaza, userName, pass);
     } catch (Exception ex) {
    	    ex.printStackTrace();
     }
    }
    

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	PrintWriter out = response.getWriter();
    	response.setContentType("text/html;charset=UTF-8");
    	try {
            if (con == null) { out.println("chyba spojenia");  return; }
            String operacia = request.getParameter("operacia");
            if (operacia == null) { zobrazNeopravnenyPristup(out); return; }
            if (operacia.equals("login")) { overUser(out, request); }
            int user_id = getUserID(request);
            if (user_id == 0) { zobrazNeopravnenyPristup(out); return; }
            vypisHlavicka(out, request);
            if (operacia.equals("pridat")) { zapisKoment(out, request); }
            else if (operacia.equals("ban")) { zabanovatPouzivatela(out, request); }
            else if (operacia.equals("odbanovat")) { odbanovat(out, request); }
            else if (operacia.equals("logout")) { urobLogout(out, request); return; }
            vypisChat(out, request);
            vypisPridat(out, request);
            formularOdbanovania(out, request);
            vypisLogout(out, request);
        } catch (Exception e) {  out.println(e); }
	}

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	protected void zobrazNeopravnenyPristup(PrintWriter out) {
		out.println("Neopravneny pristup");
	}
	
	protected void odbanovat(PrintWriter out, HttpServletRequest request) {
		 try {
		      Statement stmt = con.createStatement();
	          String sql = "DELETE FROM ban WHERE user_id IN(SELECT id FROM users WHERE  meno = '" + request.getParameter("banned_users") + "')";
	          stmt.executeUpdate(sql);

		    } catch (Exception e) {
		        out.println(e);
		    }
	 }
	
	protected void formularOdbanovania(PrintWriter out, HttpServletRequest request) {
		try {
	
            Statement stmt = con.createStatement();
            HttpSession session = request.getSession();
            
            ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) as 'pocet' FROM users WHERE id IN (SELECT user_id FROM ban) AND id != '" + session.getAttribute("ID") + "' ");
            rs2.next();
            
            out.println("<br/>");
            out.println("<h3>Zabanovany pouzivatelia:</h3>");     
            if(rs2.getInt("pocet") > 0) {
            	String sql = 
                	    "SELECT id, meno " +
                	    "FROM users " +
                	    "WHERE id IN (SELECT user_id FROM ban) AND id != '" + session.getAttribute("ID") + "' ";
                ResultSet rs = stmt.executeQuery(sql);
                
            	out.println("<form method='post' action='Servlet_Main'>");
				out.println("<select name='banned_users'>");
	            while (rs.next()) {
	                String user = rs.getString("meno");   
	                out.println("<option value='" + user + "'>" + user + "</option>");
	            }
	            out.println("</select>");
	            out.println("<br/></br>");
	    		out.println("<input type='hidden' name='operacia' value='odbanovat'>");
	    		out.println("<input type='submit' value='odbanovat'>");
	    		out.println("</form>");
            }else {
            	out.println("Ziadny pouzivatel nieje zabanovany");
            }
            
        } catch (Exception e) {
            out.println(e.getMessage());
        }
	}

	
	protected void zabanovatPouzivatela(PrintWriter out, HttpServletRequest request) {
		try {
			Statement stmt = con.createStatement();
			String sql = "INSERT INTO ban(user_id) VALUES ((SELECT id FROM users WHERE meno = '" + request.getParameter("user_name") + "'))";
			stmt.executeUpdate(sql);
		}catch(Exception e) {out.println(e.getMessage());} 
	}
			
	protected void overUser(PrintWriter out, HttpServletRequest request) {
		try {
			String meno = request.getParameter("login");
			String heslo = request.getParameter("pwd");
			Statement stmt = con.createStatement();
			String sql = "SELECT MAX(id) AS iid, COUNT(id) AS pocet FROM users" + " WHERE meno='"+meno+"' AND heslo = '" + heslo + "'";
			ResultSet rs = stmt.executeQuery(sql);
			rs.next();
			HttpSession session = request.getSession();
			if(rs.getInt("pocet") == 1) {
				sql = "SELECT id, meno FROM users WHERE meno = '"+meno+"'";
				rs = stmt.executeQuery(sql);
				rs.next();
				session.setAttribute("ID", rs.getInt("id"));
				session.setAttribute("meno", rs.getString("meno"));

			} else {
				out.println("Autorizacia sa nepodarila. Skontroluj prihlasovacie udaje.");
				session.invalidate();
			}
			rs.close();
			stmt.close();
		}catch(Exception e) {out.println(e.getMessage());} 
	}
	
	protected int getUserID(HttpServletRequest request) {
		HttpSession session = request.getSession();
		Integer id = (Integer)(session.getAttribute("ID"));
		if(id==null) id = 0;
		return id;  
	}
	
	
	protected void vypisHlavicka(PrintWriter out, HttpServletRequest request) {
		HttpSession session = request.getSession();
		out.println("<h2>"+session.getAttribute("meno") + "</h2>");
	}
	
	
	////////////////////
	protected void vypisChat(PrintWriter out, HttpServletRequest request) {
        out.println("<h2>Chat:</h2>");
        out.println("<table border='1'>");
		try {
			Statement stmt= con.createStatement();
			String sql = "SELECT * FROM messages " +
                    "INNER JOIN users ON users.meno = messages.meno " +
                    "WHERE users.id NOT IN (SELECT user_id FROM ban)";
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()) {
				out.println("<tr>");
				out.println("<form method='post' action=Servlet_Main>");
				out.println("<td>" + rs.getString("meno") + "</td>");
				out.println("<td>" + rs.getString("prispevok") + "</td>");		
				out.println("<input type='hidden' name='operacia' value='ban'>");
				out.println("<input type='hidden' name='user_name' value='" + rs.getString("meno") + "'>");
				out.println("<td><input type='submit' value='ban'></td>");
				out.println("</form>");
				out.println("</tr>");
				}
				out.println("</table>");

			}catch (Exception ex) {
				out.println(ex.getMessage());
		}
	}
	
	protected void vypisPridat(PrintWriter out, HttpServletRequest request) {
		out.println("</br>");
		out.println("<form action='Servlet_Main' method='post'>");
        out.println("<input type='text' name='komentar'>");
        out.println("<input type='hidden' name='operacia' value='pridat'>");
        out.println("<input type='submit' value='pridat komentar'>");
        out.println("</form>");
		
	}
	
	protected void vypisLogout(PrintWriter out, HttpServletRequest request) {
		
		out.println("<br/><br/>");
		out.println("<form method='post' action='Servlet_Main'>");
		out.println("<input type='hidden' name='operacia' value='logout'>");
		out.println("<input type='submit' value='logout'>");
		out.println("</form>");
	}
	/////////////////////////
	protected void zapisKoment(PrintWriter out, HttpServletRequest request) {
		try {
			Statement stmt = con.createStatement();
			HttpSession session = request.getSession();
			String sql = "INSERT INTO messages(meno, prispevok) VALUES ('" + session.getAttribute("meno") + "', '" + request.getParameter("komentar") + "')";
			stmt.executeUpdate(sql);
			stmt.close();
		}catch (Exception ex) {
			out.println(ex.getMessage());
		}	
	}

	protected void urobLogout(PrintWriter out, HttpServletRequest request) {
		HttpSession session = request.getSession();
		session.invalidate();
		out.println("bye bye<br>");
		out.println("<a href='index.html'>Domov</a>");
	}

	@Override
	public void destroy() {
	  try {
          con.close();
	  } catch (Exception ex) {}
	  super.destroy();
	}
}
