/*
 ***************************************************************
 *                                                             *
 *                           NOTICE                            *
 *                                                             *
 *   THIS SOFTWARE IS THE PROPERTY OF AND CONTAINS             *
 *   CONFIDENTIAL INFORMATION OF INFOR AND/OR ITS AFFILIATES   *
 *   OR SUBSIDIARIES AND SHALL NOT BE DISCLOSED WITHOUT PRIOR  *
 *   WRITTEN PERMISSION. LICENSED CUSTOMERS MAY COPY AND       *
 *   ADAPT THIS SOFTWARE FOR THEIR OWN USE IN ACCORDANCE WITH  *
 *   THE TERMS OF THEIR SOFTWARE LICENSE AGREEMENT.            *
 *   ALL OTHER RIGHTS RESERVED.                                *
 *                                                             *
 *   (c) COPYRIGHT 2020 INFOR.  ALL RIGHTS RESERVED.           *
 *   THE WORD AND DESIGN MARKS SET FORTH HEREIN ARE            *
 *   TRADEMARKS AND/OR REGISTERED TRADEMARKS OF INFOR          *
 *   AND/OR ITS AFFILIATES AND SUBSIDIARIES. ALL RIGHTS        *
 *   RESERVED.  ALL OTHER TRADEMARKS LISTED HEREIN ARE         *
 *   THE PROPERTY OF THEIR RESPECTIVE OWNERS.                  *
 *                                                             *
 ***************************************************************
 */
 
 import groovy.lang.Closure
 
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.ZoneId;
 import groovy.json.JsonSlurper;
 
 
 /*
 *Modification area - M3
 *Nbr               Date      User id     Description
 *EXT019            20230322  WZHAO       Work Order Integrated with Parts Portal 
 *
 */
 
  /*
  * Search item
 */
public class SearchItem extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
  private String cono;
  private String type;
  private String sstr;
  private String whlo;
  private String wild;
  private String itty;
  private String ledt;
  
  private int XXCONO;
  private int currentDate;
  private int currentTime
  
  private List lstMMS200MI_SearchItem;
  private List lstMDBREADMI_LstMITPOP60;
  private List lstMDBREADMI_LstMITVEN10;
  private List lstValidItems;
  
  private String itno;
  private String itds;
  private String fuds;
  private String suno_MITBAL;
  private String aval;
  private String popn;
  private String pldt;
  
  private List lstMITVEN;
  
  public SearchItem(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
    this.mi = mi;
    this.database = database;
    this.miCaller = miCaller;
    this.logger = logger;
    this.program = program;
    this.ion = ion;
  }
  
  public void main() {
    //Fetch input fields from MI
    cono = mi.inData.get("CONO") == null ? '' : mi.inData.get("CONO").trim();
  	if (cono == "?") {
  	  cono = "";
  	} 
    whlo = mi.inData.get("WHLO") == null ? '' : mi.inData.get("WHLO").trim();
  	if (whlo == "?") {
  	  whlo = "";
  	} 
  	type = mi.inData.get("TYPE") == null ? '' : mi.inData.get("TYPE").trim();
  	if (type == "?") {
  	  type = "";
  	} 
  	sstr = mi.inData.get("SSTR") == null ? '' : mi.inData.get("SSTR").trim();
  	if (sstr == "?") {
  	  sstr = "";
  	} 
  	wild = mi.inData.get("WILD") == null ? '' : mi.inData.get("WILD").trim();
  	if (wild == "?") {
  	  wild = "";
  	} 
    if (!validateInput()) {
      return;
    }
    
    currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    
    lstValidItems = new ArrayList();
    // Search on item master
    if (type.toInteger() == 1) {
      lstMMS200MI_SearchItem = new ArrayList();
      String sqry = "";
      if (wild.toInteger() == 1) {
        sqry = "STAT:[20 TO 50] (ITDS:*" + sstr + "* OR ITNO:*" + sstr + "*)";
      } else {
        sqry = "STAT:[20 TO 50] ITDS:" + sstr + "* OR ITNO:" + sstr + ")";
      }
      def  params = [ "SQRY": sqry]; 
      def callback = {
        Map<String, String> response ->
        if (response.ITNO != null) {
          String itno = response.ITNO;
          String itds = response.ITDS;
          String fuds = response.FUDS;
          def map = [ITNO: itno, ITDS: itds, FUDS: fuds];
          lstMMS200MI_SearchItem.add(map);
        }
      }
      miCaller.call("MMS200MI","SearchItem", params, callback);
      logger.debug("lstMMS200MI_SearchItem.size()=" + lstMMS200MI_SearchItem.size());
      if (lstMMS200MI_SearchItem.size() <= 0) {
        sqry = "STAT:[20 TO 50] (ITNO:*" + sstr + "* OR ITNO:" + sstr + ")";
        params = [ "SQRY": sqry];
        miCaller.call("MMS200MI","SearchItem", params, callback);
      }
      if (lstMMS200MI_SearchItem.size() > 0) {
        getStocksAndSupplierItem(lstMMS200MI_SearchItem);
      }
    } else if (type.toInteger() == 2) {
      lstMDBREADMI_LstMITPOP60 = new ArrayList();
      String sqry = "";
      String url = "";
      Map<String,String> params;
      Map<String,String> headers = ["Accept": "application/json"];
      
      boolean hasDash = false;
      if (sstr.indexOf("-") > -1) {
        hasDash = true;
      }
      // - if there is dash "-" in the searching string, wildcard search is not working, so use lst instead of
      if (hasDash) {
        String space = " ";
        String blankSpace = "";
        url = "/M3/m3api-rest/v2/execute/MDBREADMI/LstMITPOP60";
         sqry = "ALWT:5 ALWQ:\"\" POPN:" + sstr;
      } else {
        if (wild.toInteger() == 1) {
          sqry = "ALWT:5 POPN:*" + sstr + "*";
        } else {
          sqry = "ALWT:5 POPN:" + sstr;
        }
        url = "/M3/m3api-rest/v2/execute/MDBREADMI/SelMITPOP60";
      }
      params = [ "SQRY":sqry];
      
      IonResponse response = ion.get(url, headers, params);
      if (response.getStatusCode() == 200) {
        JsonSlurper jsonSlurper = new JsonSlurper();
        Map<String, Object> miResponse = (Map<String, Object>) jsonSlurper.parseText(response.getContent());
        ArrayList<Map<String, Object>> results = (ArrayList<Map<String, Object>>) miResponse.get("results");
       
        ArrayList<Map<String, String>> recordList = (ArrayList<Map<String, String>>) results[0]["records"];
        recordList.eachWithIndex { it, index ->
          Map<String, String> recordMITPOP = (Map<String, String>) it;
  	      if (recordMITPOP.ITNO != null) {
            String itno = recordMITPOP.ITNO;
            String popn = recordMITPOP.POPN;
            String e0pa = recordMITPOP.E0PA;
            boolean ok = false;
            if (hasDash) {
              if (popn.indexOf(sstr) == 0) {
                ok = true;
              }
            } else {
              ok = true;
            }
            if (ok) {
              def map = [ITNO: itno, POPN: popn, E0PA: e0pa];
              lstMDBREADMI_LstMITPOP60.add(map);
            }
          }
        }
        if (lstMDBREADMI_LstMITPOP60.size() > 0) {
          getStocksAndSupplierItem(lstMDBREADMI_LstMITPOP60);
        }
      }
    } else if (type.toInteger() == 3) {
      lstMDBREADMI_LstMITVEN10 = new ArrayList();
      Map<String,String> headers = ["Accept": "application/json"];
      Map<String,String> params = [ "SUNO":sstr];
      String url = "/M3/m3api-rest/v2/execute/MDBREADMI/LstMITVEN10";
      
      IonResponse response = ion.get(url, headers, params);
      if (response.getStatusCode() == 200) {
        JsonSlurper jsonSlurper = new JsonSlurper();
        Map<String, Object> miResponse = (Map<String, Object>) jsonSlurper.parseText(response.getContent());
        ArrayList<Map<String, Object>> results = (ArrayList<Map<String, Object>>) miResponse.get("results");
       
        ArrayList<Map<String, String>> recordList = (ArrayList<Map<String, String>>) results[0]["records"];
        recordList.eachWithIndex { it, index ->
          Map<String, String> recordMITVEN = (Map<String, String>) it;
  	      if (recordMITVEN.ITNO != null) {
  	        String suno = recordMITVEN.SUNO;
            String itno = recordMITVEN.ITNO;
            String ppun = recordMITVEN.PPUN;
            String pupr = recordMITVEN.PUPR.trim().toDouble().toString();
            def map = [SUNO: suno, ITNO: itno, PPUN: ppun, PUPR: pupr];
            lstMDBREADMI_LstMITVEN10.add(map);
          }
        }
        if (lstMDBREADMI_LstMITVEN10.size() > 0) {
          getStocksAndSupplierItem(lstMDBREADMI_LstMITVEN10);
        }
      }
    }
    if (lstValidItems.size() > 0) {
      for (int i=0;i<lstValidItems.size();i++) {
        Map<String, String> record = (Map<String, String>) lstValidItems[i];
        mi.outData.put("ITNO", record.ITNO.trim());
        mi.outData.put("ITDS", record.ITDS.trim());
        mi.outData.put("FUDS", record.FUDS.trim());
        mi.outData.put("UNMS", record.UNMS.trim());
        mi.outData.put("POPN", record.POPN.trim());
        mi.outData.put("AVAL", record.AVAL.trim());
        mi.outData.put("PPUN", record.PPUN.trim());
        mi.outData.put("PUPR", record.PUPR.trim());
        mi.outData.put("SUNO", record.SUNO.trim());
        mi.outData.put("STCD", record.STCD.trim());
        mi.outData.put("WHSL", record.WHSL.trim());
        mi.outData.put("REOP", record.REOP.trim());
        mi.outData.put("MXST", record.MXST.trim());
        mi.outData.put("ORQT", record.ORQT.trim());
        mi.outData.put("ITTY", record.ITTY.trim());
        mi.outData.put("PLDT", record.PLDT.trim());
        mi.outData.put("LEAT", record.LEAT.trim());
        mi.outData.put("WDNO", record.WDNO.trim());    
        if(record.LEDT != null){
          mi.outData.put("LEDT", record.LEDT.trim());  
        }
        mi.outData.put("WHLO", record.WHLO.trim());
        mi.write();
      }
    }
  }
  /*
   * validateInput - Validate all the input fields
   * @return false if there is any error
   *         true if pass the validation
   */
  boolean validateInput(){
	
  	if (!cono.isEmpty() ){
  	  if (cono.isInteger()){
    		XXCONO= cono.toInteger();
    	  } else {
    		mi.error("Company " + cono + " is invalid");
    		return false;
  	  }
  	  
  	} else {
  	  XXCONO= program.LDAZD.CONO;
  	}
  
  	if (whlo.isEmpty()){
  	  mi.error("Warehouse must be entered");
  	  return false;
  	}
  	DBAction queryMITWHL = database.table("MITWHL").index("00").selection("MWWHLO").build();
    DBContainer MITWHL = queryMITWHL.getContainer();
    MITWHL.set("MWCONO", XXCONO);
    MITWHL.set("MWWHLO", whlo);
    if (!queryMITWHL.read(MITWHL)) {
      mi.error("Warehouse is invalid.");
      return false;
    }
    
    if (type.isEmpty()){
  	  mi.error("Search type must be entered.");
  	  return false;
  	}
  	if (sstr.isEmpty()){
  	  mi.error("Search string must be entered.");
  	  return false;
  	} else if (sstr.length() < 3) {
  	  mi.error("Search string must have the minimun 3 characters.");
  	  return false;
  	}
  	if (wild.isEmpty()) {
  	  wild = "0";
  	}
  	if (wild.toInteger() > 0) {
  	  wild = "1";
  	}
    return true;
  }
  /*
   * getStocksAndSupplierItem - retrieve from MITBAL and MITVEN
   *
   */
  def getStocksAndSupplierItem (List itemList) {
    for (int i=0;i<itemList.size();i++) {
      Map<String, String> record = (Map<String, String>) itemList[i];
		  itno = record.ITNO.trim();
		  
		  String unms = "";
		  String itds = "";
		  String fuds = "";
		  String stcd = "";
		  DBAction queryMITMAS = database.table("MITMAS").index("00").selection("MMITDS", "MMFUDS","MMUNMS", "MMSTCD", "MMITTY").build();
      DBContainer MITMAS = queryMITMAS.getContainer();
      MITMAS.set("MMCONO", XXCONO);
      MITMAS.set("MMITNO", itno);
      if (queryMITMAS.read(MITMAS)) {
        itds = MITMAS.get("MMITDS").toString().trim();
        fuds = MITMAS.get("MMFUDS").toString().trim();
        unms = MITMAS.get("MMUNMS").toString().trim();
        stcd = MITMAS.get("MMSTCD").toString().trim();
        itty = MITMAS.get("MMITTY").toString().trim();
      }
      
		String stqt = "";
    	String alqt = "";
    	aval = "";
    	suno_MITBAL = "";
    	String whsl = "";
    	String reop = "";
    	String mxst = "";
    	String orqt = "";
        String leat = "";
    	DBAction queryMITBAL = database.table("MITBAL").index("00").selection("MBSTQT", "MBALQT", "MBSUNO", "MBWHSL", "MBREOP", "MBMXST", "MBORQT","MBLEAT").build();
      DBContainer MITBAL = queryMITBAL.getContainer();
      MITBAL.set("MBCONO", XXCONO);
      MITBAL.set("MBWHLO", whlo);
      MITBAL.set("MBITNO", itno);
      if (queryMITBAL.read(MITBAL)) {
        stqt = MITBAL.get("MBSTQT").toString().trim();
        alqt = MITBAL.get("MBALQT").toString().trim();
        aval = (stqt.toDouble() - alqt.toDouble()).toString();
        suno_MITBAL = MITBAL.get("MBSUNO").toString().trim();
        whsl = MITBAL.get("MBWHSL").toString().trim();
        reop = MITBAL.get("MBREOP").toString().trim();
        mxst = MITBAL.get("MBMXST").toString().trim();
        orqt = MITBAL.get("MBORQT").toString().trim();
        leat = MITBAL.get("MBLEAT").toString().trim();
        if(leat == null || leat == "" || leat == "0"){
          leat = "1";
        }
      } 
      
      popn = "";
      DBAction queryMITPOP = database.table("MITPOP").index("00").selection("MPPOPN").build();
      DBContainer MITPOP = queryMITPOP.getContainer();
  	  MITPOP.set("MPCONO", XXCONO);
  	  MITPOP.set("MPALWT", 5);
  	  MITPOP.set("MPALWQ", "");
  	  MITPOP.set("MPITNO", itno);
    	queryMITPOP.readAll(MITPOP, 4, 1, listMITPOP);
  	  
  	  pldt = "";
  	  DBAction queryMITPLO = database.table("MITPLO").index("00").selection("MOPLDT").build();
      DBContainer MITPLO = queryMITPLO.getContainer();
  	  MITPLO.set("MOCONO", XXCONO);
  	  MITPLO.set("MOWHLO", whlo);
  	  MITPLO.set("MOITNO", itno);
    	queryMITPLO.readAll(MITPLO, 3, listMITPLO);
  	  
  	  String suno = "";
      String pupr = "";
      String ppun = "";
      
      if (type.toInteger() == 3) {
        suno = record.SUNO.trim();
        ppun = record.PPUN.trim();
        pupr = record.PUPR.trim().toDouble().toString();
      } else {
  		  lstMITVEN = new ArrayList();
      	DBAction queryMITVEN = database.table("MITVEN").index("00").selection("IFSUNO", "IFPPUN", "IFPUPR").build();
        DBContainer MITVEN = queryMITVEN.getContainer();
        MITVEN.set("IFCONO", XXCONO);
        MITVEN.set("IFITNO", itno);
        queryMITVEN.readAll(MITVEN, 2, 99, listMITVEN);
        if (lstMITVEN.size() == 1) {
          Map<String, String> record1 = (Map<String, String>) lstMITVEN[0];
          suno = record1.SUNO;
          pupr = record1.PUPR;
          ppun = record1.PPUN;
        } else if (lstMITVEN.size() > 1) {
          for (int j=0;j<lstMITVEN.size();j++) {
            Map<String, String> record2 = (Map<String, String>) lstMITVEN[j];
            if (record2.SUNO.equals(suno_MITBAL)) {
              suno = record2.SUNO;
              pupr = record2.PUPR.trim().toDouble().toString();
              ppun = record2.PPUN; 
              break;
            } 
          }
        }
      }
      
      String wdno = "";
      DBAction queryCSYCAL = database.table("CSYCAL").index("00").selection("CDWDNO").build();
      DBContainer CSYCAL = queryCSYCAL.getContainer();
      CSYCAL.set("CDCONO", XXCONO);
      CSYCAL.set("CDDIVI", "");
      CSYCAL.set("CDYMD8", currentDate);
      if(queryCSYCAL.read(CSYCAL)){
        wdno = CSYCAL.get("CDWDNO").toString().trim();
      }
      
      if(wdno != null && wdno != "" && leat != null && leat != ""){
          wdno = (wdno.toInteger() + leat.toInteger()).toString();  
          if(wdno != null){
            Map<String,String> headers = ["Accept": "application/json"];
            Map<String,String> params = [ "CDDIVI":"", "CDWDNO": wdno];
            String url = "/M3/m3api-rest/v2/execute/CMS100MI/Lst_CSYCAL_Z01";
            IonResponse response = ion.get(url, headers, params);
            if (response.getStatusCode() == 200) {
              JsonSlurper jsonSlurper = new JsonSlurper();
              Map<String, Object> miResponse = (Map<String, Object>) jsonSlurper.parseText(response.getContent());
              ArrayList<Map<String, Object>> results = (ArrayList<Map<String, Object>>) miResponse.get("results");
              ArrayList<Map<String, String>> recordList = (ArrayList<Map<String, String>>) results[0]["records"];
              recordList.eachWithIndex { it, index ->
                Map<String, String> recordCSYCAL = (Map<String, String>) it;
        	      if (recordCSYCAL.CDYMD8 != null) {
        	        ledt = recordCSYCAL.CDYMD8;
                }
              }
            }
          }
        }
     
      def map = [ITNO: itno, ITDS: itds, FUDS: fuds, UNMS: unms, STCD: stcd, AVAL: aval, WHSL: whsl, REOP: reop, MXST: mxst, ORQT: orqt, POPN: popn, PLDT: pldt, SUNO: suno, PUPR: pupr, PPUN: ppun, ITTY: itty, LEAT: leat, WDNO: wdno, LEDT: ledt, WHLO: whlo];
      lstValidItems.add(map);
    }
  }
  /*
  * listMITPOP - Callback function to return MITPOP
  *
  */
  Closure<?> listMITPOP = { DBContainer MITPOP ->
    popn = MITPOP.get("MPPOPN").toString().trim();
  }
   /*
  * listMITPLO - Callback function to return MITPLO
  *
  */
  Closure<?> listMITPLO = { DBContainer MITPLO ->
    pldt = MITPLO.get("MOPLDT").toString().trim();
  }
  /*
  * listMITVEN - Callback function to return MITVEN
  *
  */
  Closure<?> listMITVEN = { DBContainer MITVEN ->
    String suno = MITVEN.get("IFSUNO").toString().trim();
    String pupr = MITVEN.get("IFPUPR").toString().trim();
    String ppun = MITVEN.get("IFPPUN").toString().trim();
    def map = [SUNO: suno, PUPR: pupr, PPUN: ppun];
    lstMITVEN.add(map);
  }
}