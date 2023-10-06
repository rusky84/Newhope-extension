 import groovy.lang.Closure;
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import groovy.json.JsonSlurper;
 import groovy.json.JsonException
 import groovy.xml.*;
 import groovy.util.*;
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.text.DecimalFormat;
 import java.time.ZoneId;
 
/**
  * ProcessVariance - Check for any variance lines in EXTVAR and recode accounting string
  *
 */
public class ProcessVariance extends ExtendM3Transaction {
  private final MIAPI mi;
  private final IonAPI ion;
  private final LoggerAPI logger;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final ProgramAPI program;
  
  private String PROC;
  private String cono;
  private String puno;
  private String pnli;
  private String pnls;
  
  private int XXCONO;
  private String noseries01;
  private int currentDate;
  private int currentTime

  public ProcessVariance(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
    this.mi = mi;
    this.database = database;
    this.miCaller = miCaller;
    this.logger = logger;
    this.program = program;
    this.ion = ion;
  }
  
  
  public void main() {
    
    XXCONO = program.LDAZD.CONO;
    cono = XXCONO.toString();
 
    
    ZoneId zid = ZoneId.of("Australia/Sydney"); 
    currentDate = LocalDate.now(zid).format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    currentTime = Integer.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
    

    DBAction queryEXTVAR = database.table("EXTVAR").index("10").selectAllFields().build();
    DBContainer EXTVAR = queryEXTVAR.getContainer();
  	EXTVAR.set("EXCONO", XXCONO);
  	EXTVAR.set("EXPROC", 0);
  	queryEXTVAR.readAll(EXTVAR, 2, listEXTVAR);
  	
  }
\
  /**
  * listEXTVAR - Callback function to return EXTVAR
  *
  */
  Closure<?> listEXTVAR = { DBContainer EXTVAR ->
    
    String divi = EXTVAR.get("EXDIVI").toString().trim();
    String yea4 = EXTVAR.get("EXYEA4").toString().trim();
    String jrno = EXTVAR.get("EXJRNO").toString().trim();
    String jsno = EXTVAR.get("EXJSNO").toString().trim();
    
    DBAction queryFGLEDG = database.table("FGLEDG").index("00").selectAllFields().build();
    DBContainer FGLEDG = queryFGLEDG.getContainer();
    FGLEDG.set("EGCONO", XXCONO);
    FGLEDG.set("EGDIVI", divi);
    FGLEDG.set("EGYEA4", yea4.toInteger());
    FGLEDG.set("EGJRNO", jrno.toInteger());
    FGLEDG.set("EGJSNO", jsno.toInteger());
    
    if (queryFGLEDG.read(FGLEDG)) {
      String vono = FGLEDG.get("EGVONO").toString().trim();
      String vtxt = FGLEDG.get("EGVTXT").toString().trim();
      
      boolean isRecoded = false;
      if (vtxt.length() > 40) {
        String pos40 = vtxt.substring(39,40);
        if (pos40.equals("X")) {
          isRecoded = true;
        }
      }
      if (isRecoded) {
        PROC = "";
        updateProcessFlag(divi, yea4, jrno, jsno, "2");
      } else {
        recodingVariance(FGLEDG);
      }
    }
  }
  
  /**
   * recodeVariance - recording the variance through APS450MI
   *
  */
  def recodingVariance(DBContainer FGLEDG) {  
    String jrno_FPLEDG = "";
    String jsno_FPLEDG = "";
    
    String divi = FGLEDG.get("EGDIVI").toString().trim();
    String yea4 = FGLEDG.get("EGYEA4").toString().trim();
    String jrno = FGLEDG.get("EGJRNO").toString().trim();
    String jsno = FGLEDG.get("EGJSNO").toString().trim();
    String vono = FGLEDG.get("EGVONO").toString().trim();
    String vser = FGLEDG.get("EGVSER").toString().trim();
    
    String ait1 = FGLEDG.get("EGAIT1").toString().trim();
    String ait2 = FGLEDG.get("EGAIT2").toString().trim();
    String ait3 = FGLEDG.get("EGAIT3").toString().trim();
    String ait4 = FGLEDG.get("EGAIT4").toString().trim();
    String ait5 = FGLEDG.get("EGAIT5").toString().trim();
    String ait6 = FGLEDG.get("EGAIT6").toString().trim();
    String ait7 = FGLEDG.get("EGAIT7").toString().trim();
    String acam = FGLEDG.get("EGACAM").toString().trim();
    String acqt = FGLEDG.get("EGACQT").toString().trim();
    String vtcd = FGLEDG.get("EGVTCD").toString().trim();
    String acdt = FGLEDG.get("EGACDT").toString().trim();
    String vtxt = FGLEDG.get("EGVTXT").toString().trim();
    
    def params = ["DIVI":divi, "YEA4":yea4, "VSER":"", "VONO":vono];
      
    Closure<?> GLS200MIcallback = {
      Map<String, String> response ->
      
      if(response.TRCD != null && response.TRCD.equals("40")) {
        jrno_FPLEDG = response.JRNO.trim();
        jsno_FPLEDG = response.JSNO.trim();
      }
    }

    miCaller.call("GLS200MI", "LstVoucherLines", params, GLS200MIcallback);
    
    DBAction queryFPLEDG = database.table("FPLEDG").index("00").selectAllFields().build();
    DBContainer FPLEDG = queryFPLEDG.getContainer();
    FPLEDG.set("EPCONO", XXCONO);
    FPLEDG.set("EPDIVI", divi);
    FPLEDG.set("EPYEA4", yea4.toInteger());
    FPLEDG.set("EPJRNO", jrno_FPLEDG.toInteger());
    FPLEDG.set("EPJSNO", jsno_FPLEDG.toInteger());
    
    if (!queryFPLEDG.read(FPLEDG)) {
      PROC = "";
      updateProcessFlag(divi, yea4, jrno, jsno, "2");
      return;
    }
    
    String suno = FPLEDG.get("EPSUNO").toString().trim();
    String sypn = FPLEDG.get("EPSPYN").toString().trim();
    String sino = FPLEDG.get("EPSINO").toString().trim();
    String inyr = FPLEDG.get("EPINYR").toString().trim();
    logger.debug("SUNO=" + suno + " SINO=" + sino + " INYR=" + inyr);
    
    if (sino.isEmpty()) {
      PROC = "";
      updateProcessFlag(divi, yea4, jrno, jsno, "2");
      return;
    }
    
    puno = "";
    pnli = "";
    pnls = "";
    
    ExpressionFactory expression = database.getExpressionFactory("FGINAE");
    expression = expression.eq("F9AIT1", ait1);
    expression = expression.and(expression.eq("F9AIT2", ait2));
    expression = expression.and(expression.eq("F9AIT3", ait3));
    expression = expression.and(expression.eq("F9AIT4", ait4));
    expression = expression.and(expression.eq("F9AIT5", ait5));
    expression = expression.and(expression.eq("F9AIT6", ait6));
    expression = expression.and(expression.eq("F9AIT7", ait7));
    expression = expression.and(expression.eq("F9ACAM", acam));
    
    //Check for an accounting line on a non-Average Cost Variance
    DBAction queryFGINAE = database.table("FGINAE").index("00").matching(expression).selection("F9PUNO", "F9PNLI", "F9PNLS").build();
    DBContainer FGINAE = queryFGINAE.getContainer();
    FGINAE.set("F9CONO", XXCONO);
    FGINAE.set("F9DIVI", divi);
    FGINAE.set("F9SUNO", suno);
    FGINAE.set("F9SINO", sino);
    FGINAE.set("F9INYR", inyr.toInteger());
    FGINAE.set("F9INIT", "12");
   
    queryFGINAE.readAll(FGINAE, 6, 1, lstFGINAE);
    
    if (puno.isEmpty() && pnli.isEmpty()) {
    //Check for an accounting line on an Average Cost Variance
      queryFGINAE = database.table("FGINAE").index("00").matching(expression).selection("F9PUNO", "F9PNLI", "F9PNLS").build();
      FGINAE = queryFGINAE.getContainer();
      FGINAE.set("F9CONO", XXCONO);
      FGINAE.set("F9DIVI", divi);
      FGINAE.set("F9SUNO", suno);
      FGINAE.set("F9SINO", sino);
      FGINAE.set("F9INYR", inyr.toInteger());
      FGINAE.set("F9INIT", "11");
   
      queryFGINAE.readAll(FGINAE, 6, 1, lstFGINAE);
    
     if (puno.isEmpty() && pnli.isEmpty()) {
      PROC = "";
      updateProcessFlag(divi, yea4, jrno, jsno, "2");
      return;
      }
    }
    
    //Check if CACCST has an accounting string for the PO line
    DBAction queryCACCST = database.table("CACCST").index("00").selection("SCAIT1", "SCAIT2", "SCAIT3", "SCAIT4", "SCAIT5", "SCAIT6", "SCAIT7").build();
    DBContainer CACCST = queryCACCST.getContainer();
    CACCST.set("SCCONO", XXCONO);
    CACCST.set("SCRIDN", puno);
    CACCST.set("SCRIDL", pnli.toInteger());
    CACCST.set("SCRIDX", pnls.toInteger());
    CACCST.set("SCORCA", "251");
    CACCST.set("SCORCA", "251");
    CACCST.set("SCEVEN", "PP10");
    CACCST.set("SCACTY", "903");
    
    String ait1_CACCST = "";
    String ait2_CACCST = "";
    String ait3_CACCST = "";
    String ait4_CACCST = "";
    String ait5_CACCST = "";
    String ait6_CACCST = "";
    String ait7_CACCST = "";
    
    if (!queryCACCST.read(CACCST)) {

      //If no CACCST is found, check if PO line is from a Work Order or is inventory and retrieve accounting strings from CRS399
      DBAction queryMPLINE = database.table("MPLINE").index("00").selection("IBPOTC", "IBRORC", "IBRORN", "IBRORL", "IBFACI", "IBRGDT", "IBITNO").build();
      DBContainer MPLINE = queryMPLINE.getContainer();
      MPLINE.set("IBCONO", XXCONO);
      MPLINE.set("IBPUNO", puno);
      MPLINE.set("IBPNLI", pnli.toInteger());
      MPLINE.set("IBPNLS", pnls.toInteger());
    
      if (!queryMPLINE.read(MPLINE)) {
        PROC = "";
        updateProcessFlag(divi, yea4, jrno, jsno, "2");
        logger.debug("PO Line not found in MPLINE - " + puno + "/" + pnli + "/" + pnls);
        return;
      } 
      String potc_MPLINE = MPLINE.get("IBPOTC").toString().trim();
      String rorc_MPLINE = MPLINE.get("IBRORC").toString().trim();
      String rorn_MPLINE = MPLINE.get("IBRORN").toString().trim();
      String rorl_MPLINE = MPLINE.get("IBRORL").toString().trim();
      String faci_MPLINE = MPLINE.get("IBFACI").toString().trim();
      String rgdt_MPLINE = MPLINE.get("IBRGDT").toString().trim();
      String itno_MPLINE = MPLINE.get("IBITNO").toString().trim();
      String DD = rgdt_MPLINE.substring(6,8);
      String MM = rgdt_MPLINE.substring(4,6);
      String YY = rgdt_MPLINE.substring(2,4);
      String rgdtDDMMYY = "${DD}${MM}${YY}";

      //Check if PO Line comes from a Repair or Subcontract Work Order - accounting event MO20-920
      if (rorc_MPLINE.equals("6") && (potc_MPLINE.equals("60") || potc_MPLINE.equals("70"))) {
        Map<String, String> rsAccounts = getRepSubWorkOrderAccounts(cono,divi,rgdtDDMMYY,itno_MPLINE,rorn_MPLINE,rorl_MPLINE,faci_MPLINE,"0");
        ait1_CACCST = rsAccounts.get("AIT1").trim();
        ait2_CACCST = rsAccounts.get("AIT2").trim();
        ait3_CACCST = rsAccounts.get("AIT3").trim();
        ait4_CACCST = rsAccounts.get("AIT4").trim();
        ait5_CACCST = rsAccounts.get("AIT5").trim();
        ait6_CACCST = rsAccounts.get("AIT6").trim();
        ait7_CACCST = rsAccounts.get("AIT7").trim();
        logger.debug("Repair or Subcontract - ait1=" + ait1_CACCST + " ait2=" + ait2_CACCST);
      } 
      else {
        //Check if PO Line comes from a Normal Work Order - accounting event MO10-912
        if (rorc_MPLINE.equals("6") && !(potc_MPLINE.equals("60") || potc_MPLINE.equals("70"))) {
          Map<String, String> woAccounts = getWorkOrderAccounts(cono,divi,rgdtDDMMYY,itno_MPLINE,rorn_MPLINE,rorl_MPLINE);
          ait1_CACCST = woAccounts.get("AIT1").trim();
          ait2_CACCST = woAccounts.get("AIT2").trim();
          ait3_CACCST = woAccounts.get("AIT3").trim();
          ait4_CACCST = woAccounts.get("AIT4").trim();
          ait5_CACCST = woAccounts.get("AIT5").trim();
          ait6_CACCST = woAccounts.get("AIT6").trim();
          ait7_CACCST = woAccounts.get("AIT7").trim();
          logger.debug("Work Order - ait1=" + ait1_CACCST + " ait2=" + ait2_CACCST);
        }
        else {
        //Check if PO Line is for an Inventory accounted item
          DBAction queryMITMAS = database.table("MITMAS").index("00").selection("MMSTCD").build();
          DBContainer MITMAS = queryMITMAS.getContainer();
          MITMAS.set("MMCONO", XXCONO);
          MITMAS.set("MMITNO", itno_MPLINE.trim());   
          if (!queryMITMAS.read(MITMAS)) {
            PROC = "";
            updateProcessFlag(divi, yea4, jrno, jsno, "2");
            logger.debug("MITMAS not found for item" + itno_MPLINE.trim());
            return;
          } 
          else {
            String stcd_MITMAS = MITMAS.get("MMSTCD").toString().trim();
            if (stcd_MITMAS.equals("1")) {
            logger.debug("Pre IPS call ${cono}");
              Map<String, String> inAccounts = getInventoryAccounts(cono,divi,rgdtDDMMYY,puno,pnli,pnls);
              ait1_CACCST = inAccounts.get("AIT1").trim();
              ait2_CACCST = inAccounts.get("AIT2").trim();
              ait3_CACCST = inAccounts.get("AIT3").trim();
              ait4_CACCST = inAccounts.get("AIT4").trim();
              ait5_CACCST = inAccounts.get("AIT5").trim();
              ait6_CACCST = inAccounts.get("AIT6").trim();
              ait7_CACCST = inAccounts.get("AIT7").trim();
              logger.debug("Inventory Accounted Item - ait1=" + ait1_CACCST + " ait2=" + ait2_CACCST);
            } 
            else {
              PROC = "";
              updateProcessFlag(divi, yea4, jrno, jsno, "2");
              logger.debug("Item does not match criteria for account simulation - " + itno_MPLINE.trim());
              return;
            }
          }
        }
      }
      
    }
    //If CACCST is found, use the accounting string from there
    else {
      ait1_CACCST = CACCST.get("SCAIT1").toString().trim();
      ait2_CACCST = CACCST.get("SCAIT2").toString().trim();
      ait3_CACCST = CACCST.get("SCAIT3").toString().trim();
      ait4_CACCST = CACCST.get("SCAIT4").toString().trim();
      ait5_CACCST = CACCST.get("SCAIT5").toString().trim();
      ait6_CACCST = CACCST.get("SCAIT6").toString().trim();
      ait7_CACCST = CACCST.get("SCAIT7").toString().trim();
      logger.debug("ait1_CACCST=" + ait1_CACCST + " ait2_CACCST=" + ait2_CACCST);
    }

    String inbn = create_APS450MI_header(divi, yea4, vser, vono, acdt);
    if (inbn != null && !inbn.isEmpty()) {
      if (vtxt.length() == 40) {
        vtxt = vtxt.substring(0, 38) + "X";
      } else {
        vtxt = formatFixedLen(vtxt, 39) + "X";
      }
      create_APS450MI_line(divi, inbn, (acam.toDouble() * (-1)).toString(), vtcd, (acqt.toDouble() * (-1)).toString(), ait1, ait2, ait3, ait4, ait5, ait6, ait7, vtxt);
      create_APS450MI_line(divi, inbn, acam, vtcd, acqt, ait1_CACCST, ait2_CACCST, ait3_CACCST, ait4_CACCST, ait5_CACCST, ait6_CACCST, ait7_CACCST, vtxt);
      validate_APS455MI_ValidByBatchNo(divi, inbn);
      updateProcessFlag(divi, yea4, jrno, jsno, "1");
    }
  }
  
  /**
   * lstFGINAE - Callback function to return FGINAE records
   *
  */
  Closure<?> lstFGINAE = { DBContainer FGINAE ->
    
    puno  = FGINAE.get("F9PUNO").toString();
    pnli  = FGINAE.get("F9PNLI").toString();
    pnls  = FGINAE.get("F9PNLS").toString();
    logger.debug("PUNO=" + puno + " PNLI=" + pnli);
  }
  /**
   * create_APS450MI_header - executing APS450MI.AddHeadRecode
   *
  */
  def String create_APS450MI_header(String divi, String yea4, String vser, String vono, String acdt) {
    logger.debug("Call APS450MI_AddHeadRecode...");
    
    String inbn = "";
    def params = [ "DIVI": divi, "YEA4": yea4, "VSER": vser, "VONO": vono, "ACDT": acdt]; 
    
    def callback = {
      Map<String, String> response ->
      inbn = response.INBN;
      logger.debug("INBN=" + inbn);
    }
    
    miCaller.call("APS450MI","AddHeadRecode", params, callback);
    
    return inbn;
  }
  /**
   * create_APS450MI_line - executing APS450MI.AddLineRecode
   *
  */
  def create_APS450MI_line(String divi, String inbn, String nlam, String vtcd, String acqt, String ait1, String ait2, String ait3, String ait4, String ait5, String ait6, String ait7, String vtxt) {
    logger.debug("Call APS450MI.AddLineRecode...");
    
    
    def params = [ "DIVI": divi, "INBN": inbn, "NLAM": nlam, "VTCD": vtcd, "ACQT": acqt, "AIT1": ait1, "AIT2": ait2, "AIT3": ait3, "AIT4": ait4, "AIT5": ait5, "AIT6": ait6, "AIT7": ait7, "VTXT": vtxt]; 
    def callback = {
    Map<String, String> response ->
      
    }
    
    miCaller.call("APS450MI","AddLineRecode", params, callback);
  }
  /**
   * formatFixedLength
   *
  */  
  def String formatFixedLen(String str, int len) {
    String strTemp = str;
    while (strTemp.length() < len) {
      strTemp += " ";
    }
    return strTemp;
  }
  
  /**
   * validate_APS455MI_Validate - executing APS455MI.ValidByBatchNo
   *
  */
  def validate_APS455MI_ValidByBatchNo(String divi, String inbn) {
    logger.debug("Call APS455MI.ValidByBatchNo...");
    def params = [ "DIVI": divi, "INBN": inbn]; 
    def callback = {
    Map<String, String> response ->
    
    }
    miCaller.call("APS455MI","ValidByBatchNo", params, callback);
  }
  /**
   * updateProcessFlag - update PROC in EXTVAR
   *
  */
  def updateProcessFlag(String divi, String yea4, String jrno, String jsno, String proc) {
    
	  DBAction actionEXTVAR = database.table("EXTVAR").index("00").build();
    DBContainer EXTVAR = actionEXTVAR.getContainer();
		EXTVAR.set("EXCONO", XXCONO);
		EXTVAR.set("EXDIVI", divi);
		EXTVAR.set("EXYEA4", yea4.toInteger());
		EXTVAR.set("EXJRNO", jrno.toInteger());
		EXTVAR.set("EXJSNO", jsno.toInteger());
		PROC = proc;
		if (!actionEXTVAR.readLock(EXTVAR, updateEXTVAR)) {
      return;
		}
  }
  /**
  * updateEXTVAR - Callback function
  *
  */
   Closure<?> updateEXTVAR = { LockedResult EXTVAR ->
    EXTVAR.set("EXPROC", PROC.toInteger());
    EXTVAR.set("EXLMDT", currentDate);
  	EXTVAR.set("EXCHNO", EXTVAR.get("EXCHNO").toString().toInteger() +1);
  	EXTVAR.set("EXCHID", program.getUser());
    EXTVAR.update();
   }
   
  /**
   * getInventoryAccounts - Get Accounting String for Inventory Item
   *
  */
  def Map<String, String> getInventoryAccounts(String Company, String Division, String FromDate, String OrderNumber, String OrderLineNumber, String PurchaseOrderLineSubnumber) {
    def endpoint = "M3/ips/service/CRS399"
    def headers = ["Accept": "application/xml", "Content-Type": "application/xml"]
    def queryParameters = (Map)null
    String SOAPBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cred=\"http://lawson.com/ws/credentials\" xmlns:get=\"http://schemas.infor.com/ips/CRS399/GetPP10910\">  <SOAP-ENV:Header>    <cred:lws>      <cred:company>${Company}</cred:company>      <cred:division></cred:division>    </cred:lws>  </SOAP-ENV:Header>  <SOAP-ENV:Body>    <get:GetPP10910>      <get:CRS399>        <get:Division>${Division}</get:Division>        <get:FromDate>${FromDate}</get:FromDate>        <get:OrderNumber>${OrderNumber}</get:OrderNumber>        <get:OrderLineNumber>${OrderLineNumber}</get:OrderLineNumber>        <get:PurchaseOrderLineSubnumber>${PurchaseOrderLineSubnumber}</get:PurchaseOrderLineSubnumber>      </get:CRS399>    </get:GetPP10910>  </SOAP-ENV:Body></SOAP-ENV:Envelope>"
    IonResponse response = ion.post(endpoint, headers, queryParameters, SOAPBody)
    if (response.getError()) {
      logger.debug("Failed calling ION API, detailed error message: ${response.getErrorMessage()}")
      def  accounts = [ "AIT1": "", "AIT2":"", "AIT3":"", "AIT4":"", "AIT5":"", "AIT6":"", "AIT7":""];
      return accounts;  
    }
    if (response.getStatusCode() != 200) {
      logger.debug("Expected status 200 but got ${response.getStatusCode()} instead")
      def  accounts = [ "AIT1": "", "AIT2":"", "AIT3":"", "AIT4":"", "AIT5":"", "AIT6":"", "AIT7":""];
      return accounts;  
    }

    String content = response.getContent()
    if (content == null) {
      logger.debug("Expected content from the request but got no content")
      def  accounts = [ "AIT1": "", "AIT2":"", "AIT3":"", "AIT4":"", "AIT5":"", "AIT6":"", "AIT7":""];
      return accounts;  
    }
    def responseContent =  new XmlSlurper().parseText(content) ;

    String AIT1 = "";
    String AIT2 = "";
    String AIT3 = "";
    String AIT4 = "";
    String AIT5 = "";
    String AIT6 = "";
    String AIT7 = "";
    
      def ResponseBody = content.toString();
      int indexStart = ResponseBody.indexOf("AccountingDimension1>");
      int indexEnd = 0;
      if (indexStart !=-1){
         AIT1=ResponseBody.substring(indexStart+20);
         indexEnd=AIT1.indexOf("<");
         AIT1=AIT1.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension2>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT2=ResponseBody.substring(indexStart+20);
         indexEnd=AIT2.indexOf("<");
         AIT2=AIT2.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension3>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT3=ResponseBody.substring(indexStart+20);
         indexEnd=AIT3.indexOf("<");
         AIT3=AIT3.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension4>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT4=ResponseBody.substring(indexStart+20);
         indexEnd=AIT4.indexOf("<");
         AIT4=AIT4.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension5>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT5=ResponseBody.substring(indexStart+20);
         indexEnd=AIT5.indexOf("<");
         AIT5=AIT5.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension6>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT6=ResponseBody.substring(indexStart+20);
         indexEnd=AIT6.indexOf("<");
         AIT6=AIT6.substring(1,indexEnd);
      }
            indexStart = ResponseBody.indexOf("AccountingDimension7>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT7=ResponseBody.substring(indexStart+20);
         indexEnd=AIT7.indexOf("<");
         AIT7=AIT7.substring(1,indexEnd);
      }

    logger.debug("reponseContent=" + responseContent);
    
    def  accounts = [ "AIT1": AIT1, "AIT2":AIT2, "AIT3":AIT3, "AIT4":AIT4, "AIT5":AIT5, "AIT6":AIT6, "AIT7":AIT7];
    return accounts;  
  }
  
  /**
   * getWorkOrderAccounts - Get Accounting String for Work Order lines
   *
  */
  def Map<String, String> getWorkOrderAccounts(String Company, String Division, String FromDate, String ItemProductNumber, String ManufacturingOrderNumber, String SequenceNumber) {
    def endpoint = "M3/ips/service/CRS399"
    def headers = ["Accept": "application/xml", "Content-Type": "application/xml"]
    def queryParameters = (Map)null
    String SOAPBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cred=\"http://lawson.com/ws/credentials\" xmlns:get=\"http://schemas.infor.com/ips/CRS399/GetMO10912\">  <SOAP-ENV:Header>    <cred:lws>      <cred:company>${Company}</cred:company>      <cred:division></cred:division>    </cred:lws>  </SOAP-ENV:Header>  <SOAP-ENV:Body>    <get:GetMO10912>      <get:CRS399>        <get:Division>${Division}</get:Division>        <get:FromDate>${FromDate}</get:FromDate>        <get:ItemProductNumber>${ItemProductNumber}</get:ItemProductNumber>        <get:SequenceNumber>${SequenceNumber}</get:SequenceNumber>        <get:ManufacturingOrderNumber>${ManufacturingOrderNumber}</get:ManufacturingOrderNumber>      </get:CRS399>    </get:GetMO10912>  </SOAP-ENV:Body></SOAP-ENV:Envelope>"
    IonResponse response = ion.post(endpoint, headers, queryParameters, SOAPBody)
    if (response.getError()) {
      logger.debug("Failed calling ION API, detailed error message: ${response.getErrorMessage()}")
      def  accounts = [ "AIT1": "", "AIT2":"", "AIT3":"", "AIT4":"", "AIT5":"", "AIT6":"", "AIT7":""];
      return accounts;  
    }
    if (response.getStatusCode() != 200) {
      logger.debug("Expected status 200 but got ${response.getStatusCode()} instead")
      def  accounts = [ "AIT1": "", "AIT2":"", "AIT3":"", "AIT4":"", "AIT5":"", "AIT6":"", "AIT7":""];
      return accounts;  
    }
    String content = response.getContent()
    if (content == null) {
      logger.debug("Expected content from the request but got no content")
      def  accounts = [ "AIT1": "", "AIT2":"", "AIT3":"", "AIT4":"", "AIT5":"", "AIT6":"", "AIT7":""];
      return accounts;  
    }
    def responseContent =  new XmlSlurper().parseText(content) ;

    String AIT1 = "";
    String AIT2 = "";
    String AIT3 = "";
    String AIT4 = "";
    String AIT5 = "";
    String AIT6 = "";
    String AIT7 = "";
    
      def ResponseBody = content.toString();
      int indexStart = ResponseBody.indexOf("AccountingDimension1>");
      int indexEnd = 0;
      if (indexStart !=-1){
         AIT1=ResponseBody.substring(indexStart+20);
         indexEnd=AIT1.indexOf("<");
         AIT1=AIT1.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension2>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT2=ResponseBody.substring(indexStart+20);
         indexEnd=AIT2.indexOf("<");
         AIT2=AIT2.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension3>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT3=ResponseBody.substring(indexStart+20);
         indexEnd=AIT3.indexOf("<");
         AIT3=AIT3.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension4>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT4=ResponseBody.substring(indexStart+20);
         indexEnd=AIT4.indexOf("<");
         AIT4=AIT4.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension5>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT5=ResponseBody.substring(indexStart+20);
         indexEnd=AIT5.indexOf("<");
         AIT5=AIT5.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension6>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT6=ResponseBody.substring(indexStart+20);
         indexEnd=AIT6.indexOf("<");
         AIT6=AIT6.substring(1,indexEnd);
      }
            indexStart = ResponseBody.indexOf("AccountingDimension7>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT7=ResponseBody.substring(indexStart+20);
         indexEnd=AIT7.indexOf("<");
         AIT7=AIT7.substring(1,indexEnd);
      }

    logger.debug("reponseContent=" + responseContent);
    
    def  accounts = [ "AIT1": AIT1, "AIT2":AIT2, "AIT3":AIT3, "AIT4":AIT4, "AIT5":AIT5, "AIT6":AIT6, "AIT7":AIT7];
    return accounts;
  }
  
  /**
   * getRepSubWorkOrderAccounts - Get Accounting String for Repair or Subcontract Work Order lines
   *
  */
  def Map<String, String> getRepSubWorkOrderAccounts(String Company, String Division, String FromDate, String ItemProductNumber, String ManufacturingOrderNumber, String OperationNumber, String Facility, String Rework) {
    def endpoint = "M3/ips/service/CRS399"
    def headers = ["Accept": "application/xml", "Content-Type": "application/xml"]
    def queryParameters = (Map)null
    String SOAPBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cred=\"http://lawson.com/ws/credentials\" xmlns:get=\"http://schemas.infor.com/ips/CRS399/GetMO20920\">  <SOAP-ENV:Header>    <cred:lws>      <cred:company>${Company}</cred:company>      <cred:division></cred:division>    </cred:lws>  </SOAP-ENV:Header>  <SOAP-ENV:Body>    <get:GetMO20920>      <get:CRS399>        <get:Division>${Division}</get:Division>        <get:FromDate>${FromDate}</get:FromDate>        <get:ItemProductNumber>${ItemProductNumber}</get:ItemProductNumber>        <get:ManufacturingOrderNumber>${ManufacturingOrderNumber}</get:ManufacturingOrderNumber>        <get:OperationNumber>${OperationNumber}</get:OperationNumber>        <get:Facility>${Facility}</get:Facility>        <get:Rework>${Rework}</get:Rework>      </get:CRS399>    </get:GetMO20920>  </SOAP-ENV:Body></SOAP-ENV:Envelope>"
    IonResponse response = ion.post(endpoint, headers, queryParameters, SOAPBody)
    if (response.getError()) {
      logger.debug("Failed calling ION API, detailed error message: ${response.getErrorMessage()}")
      def  accounts = [ "AIT1": "", "AIT2":"", "AIT3":"", "AIT4":"", "AIT5":"", "AIT6":"", "AIT7":""];
      return accounts;  
    }
    if (response.getStatusCode() != 200) {
      logger.debug("Expected status 200 but got ${response.getStatusCode()} instead")
      def  accounts = [ "AIT1": "", "AIT2":"", "AIT3":"", "AIT4":"", "AIT5":"", "AIT6":"", "AIT7":""];
      return accounts;  
    }
    String content = response.getContent()
    if (content == null) {
      logger.debug("Expected content from the request but got no content")
      def  accounts = [ "AIT1": "", "AIT2":"", "AIT3":"", "AIT4":"", "AIT5":"", "AIT6":"", "AIT7":""];
      return accounts;  
    }
    def responseContent =  new XmlSlurper().parseText(content) ;

    String AIT1 = "";
    String AIT2 = "";
    String AIT3 = "";
    String AIT4 = "";
    String AIT5 = "";
    String AIT6 = "";
    String AIT7 = "";
    
      def ResponseBody = content.toString();
      int indexStart = ResponseBody.indexOf("AccountingDimension1>");
      int indexEnd = 0;
      if (indexStart !=-1){
         AIT1=ResponseBody.substring(indexStart+20);
         indexEnd=AIT1.indexOf("<");
         AIT1=AIT1.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension2>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT2=ResponseBody.substring(indexStart+20);
         indexEnd=AIT2.indexOf("<");
         AIT2=AIT2.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension3>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT3=ResponseBody.substring(indexStart+20);
         indexEnd=AIT3.indexOf("<");
         AIT3=AIT3.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension4>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT4=ResponseBody.substring(indexStart+20);
         indexEnd=AIT4.indexOf("<");
         AIT4=AIT4.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension5>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT5=ResponseBody.substring(indexStart+20);
         indexEnd=AIT5.indexOf("<");
         AIT5=AIT5.substring(1,indexEnd);
      }
      indexStart = ResponseBody.indexOf("AccountingDimension6>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT6=ResponseBody.substring(indexStart+20);
         indexEnd=AIT6.indexOf("<");
         AIT6=AIT6.substring(1,indexEnd);
      }
            indexStart = ResponseBody.indexOf("AccountingDimension7>");
      indexEnd = 0;
      if (indexStart !=-1){
         AIT7=ResponseBody.substring(indexStart+20);
         indexEnd=AIT7.indexOf("<");
         AIT7=AIT7.substring(1,indexEnd);
      }

    logger.debug("reponseContent=" + responseContent);
    
    def  accounts = [ "AIT1": AIT1, "AIT2":AIT2, "AIT3":AIT3, "AIT4":AIT4, "AIT5":AIT5, "AIT6":AIT6, "AIT7":AIT7];
    return accounts;
  }
}
