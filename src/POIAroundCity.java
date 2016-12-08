/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
//package linkedgeodata.org.poi;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.jcraft.jsch.*;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import com.centerkey.utils.BareBonesBrowserLaunch;

/**
 *
 * @author sadnana
 */
public class POIAroundCity extends javax.swing.JFrame {

    
    //sparql endpoint
    final static String serviceEndpoint = "http://linkedgeodata.org/sparql";
    //map to hold citynames and their latiutde and longitude
    static Map<String, double[]> cityNameLatLong = new HashMap<String, double[]>();
    //POINT OF INTEREST
    static List<String> pointInterest = new ArrayList<String>();
    //radius from center of city
    int[] radiusInKM = new int[100];

    
    
    /**
     * Creates new form POIAroundCity
     */
    public POIAroundCity() {
        initComponents();
        
        //count the number of cities that is in english @en
        String qNumOfCity =
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX lgdo: <http://linkedgeodata.org/ontology/>" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
            "SELECT distinct count(?member) WHERE {" +
            "                   ?class rdf:type lgdo:City. " +
            "                   ?member  a      lgdo:City; " +
            "                   rdfs:label ?cityname. " +
            "           FILTER(lang(?cityname)=\"en\") . " +
            "}";

   
        com.hp.hpl.jena.query.Query query = QueryFactory.create(qNumOfCity, Syntax.syntaxARQ);
        QueryExecution qe= QueryExecutionFactory.sparqlService(serviceEndpoint, query);
 
        //contains the number of cities
        int numOfCity = 0;
        
        //getting the results for number of cities
	try {
	     ResultSet rs = qe.execSelect();
             if(rs.hasNext() ) {
	               
	                // get the column variable name
	            	List<String> countVar = rs.getResultVars(); 
	            	QuerySolution qs = rs.next();	
                        //extract the number of cities
	            	numOfCity = qs.getLiteral(countVar.get(0)).getInt();
                        String cities = String.valueOf(numOfCity);
	                //type of the node
                        //System.out.println(qs.getLiteral(countVar.get(0)));
                        //number of cities
	                //System.out.println("No : " + numOfCity);
                        LabelnumOfCity.setText(cities+" cities found");
	            }
	        }
	        catch(Exception e) {
	            System.out.println(e.getMessage());
	        }       
	        finally {
	            qe.close();
	        }
        
        
        /**
         * loop through all the cities to retrieve the city names
         * each page contains maximum of 1000 results, hence
         * add offset of 1000 till the number of cities are exhausted
         */
        
        //for(int i = 0 ; i < numOfCity; i+=1000){
        for(int i = 0 ; i <= 1000; i+=1000){
            String qAllCities =
                    
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX lgdo: <http://linkedgeodata.org/ontology/>" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
            "SELECT distinct ?cityname ?lat ?long WHERE {" +
            "           ?class rdf:type lgdo:City. " +
            "           ?member     a      lgdo:City; " +
            "                   rdfs:label ?cityname; " +
            "                   <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat; " +
            "                   <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?long. " +
            "           FILTER(lang(?cityname)=\"en\") . " +
            "   }LIMIT 1000 OFFSET "+i;

        query = QueryFactory.create(qAllCities, Syntax.syntaxARQ);
        qe= QueryExecutionFactory.sparqlService(serviceEndpoint, query);
       
        int count = 1;
        try{
            ResultSet rs = qe.execSelect();

            while(rs.hasNext()){
                //get each line from result set
                QuerySolution qs = rs.nextSolution();

                //get the nodes
                RDFNode cityName = qs.get("cityname");
                RDFNode latitude = qs.get("lat");
                RDFNode longitude = qs.get("long");
                //RDFNode member = qs.get("member");

                //convert to appropriate datatype
                String city = ((Literal)cityName).getString();
                double lat = ((Literal)latitude).getDouble();
	        double lon = ((Literal)longitude).getDouble();

                cityNameLatLong.put(city, new double[]{lat, lon});

                System.out.println(count+i +" - "+ cityName+" : " + lat + " : " + lon);
                count++;

            }

        }catch(Exception e){
            System.out.println(e.getMessage());
        }        
        
        }//for loop ends here
        
        
        /**
         * populate the combo box for city names 
         */
        DefaultComboBoxModel dcm = new DefaultComboBoxModel();
        jComboBoxCityName.setModel(dcm);
        //load all the cities
        for(String elem : cityNameLatLong.keySet())
            dcm.addElement(elem);

        /**
         * count the point of interests
         */
              
        String qNumPOI =
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX lgdo: <http://linkedgeodata.org/ontology/>" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
            "SELECT distinct count(?poi) WHERE {" +
            "                   ?poi ?p ?o. " +
//            "                   ?member  a      lgdo:City; " +
//            "                   rdfs:label ?cityname; " +
//            "           FILTER(lang(?cityname)=\"en\") . " +
            "}";

   
        query = QueryFactory.create(qNumPOI, Syntax.syntaxARQ);
        qe= QueryExecutionFactory.sparqlService(serviceEndpoint, query);

               //contains the number of cities
        long numOfPOI = 0;
        
        //getting the results for number of cities
	try {
	     ResultSet rs = qe.execSelect();
             if(rs.hasNext() ) {
	               
	                // get the column variable name
	            	List<String> countVar = rs.getResultVars(); 
	            	QuerySolution qs = rs.next();	
                        //extract the number of cities
	            	numOfPOI = qs.getLiteral(countVar.get(0)).getInt();
                        String poi = String.valueOf(numOfPOI);
	                //type of the node
                        //System.out.println(qs.getLiteral(countVar.get(0)));
                        //number of cities
	                //System.out.println("No : " + numOfCity);
                        jLabelPOI.setText(poi+" POIs found");
	            }
	        }
	        catch(Exception e) {
	            System.out.println(e.getMessage());
	        }       
	        finally {
	            qe.close();
	        }
        
        
        
      /**
         * loop through all the POIs to retrieve the POI names
         * each page contains maximum of 1000 results, hence
         * add offset of 1000 till the number of POIs are exhausted
         */
        
        //for(int i = 0 ; i < numOfCity; i+=1000){
        for(int i = 0 ; i <= 1000; i+=1000){
                                         
         String qPOINames =
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX lgdo: <http://linkedgeodata.org/ontology/>" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
            "SELECT distinct ?poi WHERE {" +
            "                   ?poi ?p ?o. " +
//            "                   ?member  a      lgdo:City; " +
//            "                   rdfs:label ?cityname; " +
//            "           FILTER(lang(?cityname)=\"en\") . " +
            "}";

        query = QueryFactory.create(qPOINames, Syntax.syntaxARQ);
        qe= QueryExecutionFactory.sparqlService(serviceEndpoint, query);
       
        int count = 1;
        try{
            ResultSet rs = qe.execSelect();
            Resource r;
            String poi = null;
            while(rs.hasNext()){
                //get each line from result set
                QuerySolution qs = rs.nextSolution();

                //get the nodes
                RDFNode poiNode = qs.get("poi");
                
                //check if it is a resource
                if(poiNode.isResource()){
                    poi = ((Resource)poiNode).getLocalName();                
                }
                //add to the list
                pointInterest.add(poi);
                
                System.out.println(count+i +" - "+ poiNode+" : " + poi);
                count++;

            }

        }catch(Exception e){
            System.out.println(e.getMessage());
        }        
        
        }//for loop ends here
        
        
        /**
        * populate the combo box for city names 
        */
        DefaultComboBoxModel dcmpoi = new DefaultComboBoxModel();
        jComboBoxPOIs.setModel(dcmpoi);
        //load all the cities
        for(String elem : pointInterest)
            dcmpoi.addElement(elem);
       
        /**
        * populate the combo box for radius from center 
        */

        DefaultComboBoxModel dcmradius = new DefaultComboBoxModel();
        jComboBoxRadius.setModel(dcmradius);
        //load 1 till 100 km 
        for(int j = 0; j < 100; j++)
            radiusInKM[j] = j+1;

        for(int rad : radiusInKM)
            dcmradius.addElement(rad);


        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jComboBoxCityName = new javax.swing.JComboBox();
        jComboBoxPOIs = new javax.swing.JComboBox();
        jComboBoxRadius = new javax.swing.JComboBox();
        LabelnumOfCity = new javax.swing.JLabel();
        jLabelPOI = new javax.swing.JLabel();
        jButtonShow = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaResult = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jComboBoxCityName.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxCityName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxCityNameActionPerformed(evt);
            }
        });

        jComboBoxPOIs.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jComboBoxRadius.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        LabelnumOfCity.setText("Number of Cities");

        jLabelPOI.setText("Point Of Interest");

        jButtonShow.setText("Show");
        jButtonShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonShowActionPerformed(evt);
            }
        });

        jLabel1.setText("Radius");

        jTextAreaResult.setColumns(20);
        jTextAreaResult.setRows(5);
        jScrollPane1.setViewportView(jTextAreaResult);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBoxCityName, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(LabelnumOfCity))
                        .addGap(93, 93, 93)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBoxPOIs, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabelPOI))
                        .addGap(56, 56, 56)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(59, 59, 59)
                                .addComponent(jComboBoxRadius, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(37, 37, 37)
                                .addComponent(jButtonShow))
                            .addComponent(jLabel1)))
                    .addComponent(jScrollPane1))
                .addContainerGap(146, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(47, 47, 47)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LabelnumOfCity, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelPOI)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxCityName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBoxPOIs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBoxRadius, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonShow))
                .addGap(37, 37, 37)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(73, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(39, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBoxCityNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxCityNameActionPerformed

        // TODO add your handling code here:
        
        
    }//GEN-LAST:event_jComboBoxCityNameActionPerformed

    private void jButtonShowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonShowActionPerformed

        
        // TODO add your handling code here:
        int radiusSelect = Integer.parseInt(jComboBoxRadius.getSelectedItem().toString());
        String citySelect = jComboBoxCityName.getSelectedItem().toString();
        String poiSelect = jComboBoxPOIs.getSelectedItem().toString();
        double cityLat = 0.0;
        double cityLong = 0.0;
        
        //find the lat and long for selected city
        for(Map.Entry<String, double[]> entry : cityNameLatLong.entrySet()){
            String k = entry.getKey();
            double[] v = entry.getValue();
            if(k.equalsIgnoreCase(citySelect)){
                cityLat = v[0];
                cityLong = v[1];
            }

        }
        
        
        /**
         * 
         * in following query, st_point(longitude comes first, then latitude)
         * 
         */
        String qFindAllEntity =

        "PREFIX lgdo: <http://linkedgeodata.org/ontology/>" +
        "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>" +
        "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>" +
        "SELECT ?entityname ?entitygeo (<bif:st_distance>(<bif:st_point> (" + cityLong +","+ cityLat + ") , ?entitygeo) AS ?distance)"+
        "FROM <http://linkedgeodata.org> WHERE {" +
        "?school     a            lgdo:"+ poiSelect +" ." +
        "?school     geo:geometry ?entitygeo ." +
        "?school     rdfs:label   ?entityname ." +
        "FILTER(<bif:st_intersects> (?entitygeo, <bif:st_point> ("+ cityLong +","+ cityLat + ")," + radiusSelect + ")) " +
        "}";
        
        com.hp.hpl.jena.query.Query query = QueryFactory.create(qFindAllEntity, Syntax.syntaxARQ);
        QueryExecution qe = QueryExecutionFactory.sparqlService(serviceEndpoint, query);

        //clears data in text area
        jTextAreaResult.setText("");

        try{
            ResultSet rs = qe.execSelect();

            while(rs.hasNext()){
                //get each line from result set
                QuerySolution qs = rs.nextSolution();

                //get the nodes
                RDFNode entityname = qs.get("entityname");
                RDFNode entitygeo = qs.get("entitygeo");
                RDFNode shortestDistance = qs.get("distance");

                //convert to appropriate datatype
                String entityName = ((Literal)entityname).getString();
                //double schoolGeo = ((Literal)schoolgeo).getDouble();
                double shortDist = ((Literal)shortestDistance).getDouble();
                System.out.println(entityname +" : " + entitygeo +" : " + shortDist +"   " + shortestDistance);               
                jTextAreaResult.append(entityName+"\t | \t"+ shortDist+" Km\n");
              
            }

        }catch(Exception e){
            System.out.println(e.getMessage());
        }

        /**
         * contruct an rdf graph to plot in google map
         */
        
        String qConstructGraph =
        "PREFIX lgdo: <http://linkedgeodata.org/ontology/>" +
        "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>" +
        "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>" +
        "CONSTRUCT {?entity rdfs:label ?entityname ;"+
        "                   geo:lat   ?lat;"+
        "                   geo:long  ?lon."+
        "}"+
        "WHERE {" +
        "?entity     a            lgdo:"+ poiSelect +" ." +
        "?entity     geo:geometry ?entitygeo ." +
        "?entity     rdfs:label   ?entityname ;" +
        "            geo:lat      ?lat ;"+
        "            geo:long      ?lon ."+
        "FILTER(<bif:st_intersects> (?entitygeo, <bif:st_point> ("+ cityLong +","+ cityLat + ")," + radiusSelect + ")) " +
        "}";

        query = QueryFactory.create(qConstructGraph, Syntax.syntaxARQ);
        qe = QueryExecutionFactory.sparqlService(serviceEndpoint, query);

        Model results = qe.execConstruct();
        
        /**
         * saving as rdf graph
         * 
         */
               //saving showmap.rdf
        //String relativeWebPath = "138.119.12.49:8080/tmp/";
        try{
        FileOutputStream fout=new FileOutputStream("showmap.rdf");
        // null forces producing RDF data 
        //save showmap.rdf in project directory
        results.write(fout, null);
        //show rdf output on console
        results.write(System.out,null);
        }
        catch(IOException e){
            System.out.println(e.getMessage());
        }

      
        
        /**
         * just for the demo
         * copying the file into server
         */
        
        //remove this part after demo
        String hostname = "138.119.12.49";
	String username = "sadnana";
	String password = "Pine@pple0749";
        
        try{
            Connection conn = new Connection(hostname);
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPassword(username, password);

		if (isAuthenticated == false)
			throw new IOException("Authentication failed.");
		/* Create a session */
		Session sess = conn.openSession();
                SCPClient scpclient = conn.createSCPClient();
                try{
		scpclient.put("showmap.rdf", "/usr/local/tomcat/webapps/ROOT/tmp");
                sess.execCommand("chmod 777 /usr/local/tomcat/webapps/ROOT/tmp/showmap.rdf");
                }
                catch(FileNotFoundException fe){
			System.out.println("file not found "+fe.getMessage());
		}
                sess.close();
		/* Close the connection */
		conn.close();
        }
 	catch (IOException e){
		System.out.println("something wrong "+e.getMessage());
		e.printStackTrace(System.err);
		System.exit(2);
	}
       
        
        
        
        
        
        
        
        
        
        /**********
         * 
         * 
         * 
         * 
         * 
         * 
         */
 /*       
		    try{
		      JSch jsch=new JSch();  

		      String host=null;
		      
		      com.jcraft.jsch.Session session=jsch.getSession("sadnana", "138.119.12.49", 22);
		      

		      // username and password will be given via UserInfo interface.
		      UserInfo ui=new MyUserInfo();
		      session.setUserInfo(ui);
		      session.connect();
                      
                      
                      //you can take a command on your own or embedd the command like below
		   ///   String command=JOptionPane.showInputDialog("Enter command", 
		   //                                              "set|grep SSH");

		      Channel channel=session.openChannel("exec");
                      String command = "chmod 777 /usr/local/tomcat/webapps/ROOT/tmp/showmap.rdf";
		      ((ChannelExec)channel).setCommand(command);

		      // X Forwarding
		      // channel.setXForwarding(true);

		      //channel.setInputStream(System.in);
		      channel.setInputStream(null);

		      //channel.setOutputStream(System.out);

		      //FileOutputStream fos=new FileOutputStream("/tmp/stderr");
		      //((ChannelExec)channel).setErrStream(fos);
		      ((ChannelExec)channel).setErrStream(System.err);

		      InputStream in=channel.getInputStream();

		      channel.connect();

		      byte[] tmp=new byte[1024];
		      while(true){
		        while(in.available()>0){
		          int i=in.read(tmp, 0, 1024);
		          if(i<0)break;
		          System.out.print(new String(tmp, 0, i));
		        }
		        if(channel.isClosed()){
		          System.out.println("exit-status: "+channel.getExitStatus());
		          break;
		        }
		        try{Thread.sleep(1000);}catch(Exception ee){}
		      }
		      channel.disconnect();
		      session.disconnect();
		    }
		    catch(Exception e){
		      System.out.println(e);
		    }
                    
    */                
                                                            //showing in the map
        //the rdf file should be already the server specified here
        //we need to make it dynamic

        BareBonesBrowserLaunch.openURL("http://graphite.ecs.soton.ac.uk/geo2kml/index.php?uri=http://138.119.12.49:8080/tmp/showmap.rdf&mode=google");

                    
		  }

		  public static class MyUserInfo implements UserInfo, UIKeyboardInteractive{
		    public String getPassword(){ return passwd; }
		    public boolean promptYesNo(String str){
		      Object[] options={ "yes", "no" };
		      int foo=JOptionPane.showOptionDialog(null, 
		             str,
		             "Warning", 
		             JOptionPane.DEFAULT_OPTION, 
		             JOptionPane.WARNING_MESSAGE,
		             null, options, options[0]);
		       return foo==0;
		    }
		  
		    String passwd;
		    JTextField passwordField=(JTextField)new JPasswordField(20);

		    public String getPassphrase(){ return null; }
		    public boolean promptPassphrase(String message){ return true; }
		    public boolean promptPassword(String message){
		      Object[] ob={passwordField}; 
		      int result=
		        JOptionPane.showConfirmDialog(null, ob, message,
		                                      JOptionPane.OK_CANCEL_OPTION);
		      if(result==JOptionPane.OK_OPTION){
		        passwd=passwordField.getText();
		        return true;
		      }
		      else{ 
		        return false; 
		      }
		    }
		    public void showMessage(String message){
		      JOptionPane.showMessageDialog(null, message);
		    }
		    final GridBagConstraints gbc = 
		      new GridBagConstraints(0,0,1,1,1,1,
		                             GridBagConstraints.NORTHWEST,
		                             GridBagConstraints.NONE,
		                             new Insets(0,0,0,0),0,0);
		    private Container panel;
		    public String[] promptKeyboardInteractive(String destination,
		                                              String name,
		                                              String instruction,
		                                              String[] prompt,
		                                              boolean[] echo){
		      panel = new JPanel();
		      panel.setLayout(new GridBagLayout());

		      gbc.weightx = 1.0;
		      gbc.gridwidth = GridBagConstraints.REMAINDER;
		      gbc.gridx = 0;
		      panel.add(new JLabel(instruction), gbc);
		      gbc.gridy++;

		      gbc.gridwidth = GridBagConstraints.RELATIVE;

		      JTextField[] texts=new JTextField[prompt.length];
		      for(int i=0; i<prompt.length; i++){
		        gbc.fill = GridBagConstraints.NONE;
		        gbc.gridx = 0;
		        gbc.weightx = 1;
		        panel.add(new JLabel(prompt[i]),gbc);

		        gbc.gridx = 1;
		        gbc.fill = GridBagConstraints.HORIZONTAL;
		        gbc.weighty = 1;
		        if(echo[i]){
		          texts[i]=new JTextField(20);
		        }
		        else{
		          texts[i]=new JPasswordField(20);
		        }
		        panel.add(texts[i], gbc);
		        gbc.gridy++;
		      }

		      if(JOptionPane.showConfirmDialog(null, panel, 
		                                       destination+": "+name,
		                                       JOptionPane.OK_CANCEL_OPTION,
		                                       JOptionPane.QUESTION_MESSAGE)
		         ==JOptionPane.OK_OPTION){
		        String[] response=new String[prompt.length];
		        for(int i=0; i<prompt.length; i++){
		          response[i]=texts[i].getText();
		        }
			return response;
		      }
		      else{
		        return null;  // cancel
		      }
                      
                      
                      
                      
                      
 
		  
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        

        
        
        
        /***
         * 
         * 
         * 
         */
               
  
        
 
    }//GEN-LAST:event_jButtonShowActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(POIAroundCity.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(POIAroundCity.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(POIAroundCity.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(POIAroundCity.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new POIAroundCity().setVisible(true);
            }
        });
        
    
		    }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel LabelnumOfCity;
    private javax.swing.JButton jButtonShow;
    private javax.swing.JComboBox jComboBoxCityName;
    private javax.swing.JComboBox jComboBoxPOIs;
    private javax.swing.JComboBox jComboBoxRadius;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelPOI;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextAreaResult;
    // End of variables declaration//GEN-END:variables
}
