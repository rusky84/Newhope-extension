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
 
 
 
 import groovy.lang.Closure;
 import groovy.json.JsonSlurper;
 
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.text.DecimalFormat;
 import java.time.ZoneId;
 
  /*
 *Modification area - M3
 *Nbr               Date       User id     Description
 *WKF009            20230927   KVERCO      Supplier invoice approval run
 *
 */
 
 public class ProcessApproval extends ExtendM3Transaction {
  private final MIAPI mi;
  private final LoggerAPI logger;
  private final DatabaseAPI database;
  private final BatchAPI batch;
  private final MICallerAPI miCaller;
  private final ProgramAPI program;
  
  private int XXCONO;
  private int currentDate;
  private int currentTime;
  
  boolean allStatusesOk;
  
  private List lstToBeApproved;

  public ProcessApproval(MIAPI mi, LoggerAPI logger, DatabaseAPI database, MICallerAPI miCaller, ProgramAPI program) {
    this.mi = mi;
    this.logger = logger;
    this.database = database;
  	this.miCaller = miCaller;
  	this.program = program;
  }
  
  public void main() {
    XXCONO= program.LDAZD.CONO;

    currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    currentTime = Integer.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
    
    lstToBeApproved = new ArrayList();
    
    ExpressionFactory expression = database.getExpressionFactory("FPLEDG");
    expression = expression.eq("EPAPRV", "0");
    
    DBAction queryFPLEDG = database.table("FPLEDG").index("00").matching(expression).selection("EPDIVI", "EPSUNO", "EPSINO", "EPINYR", "EPAPRV").build();
    DBContainer FPLEDG = queryFPLEDG.getContainer();
    FPLEDG.set("EPCONO", XXCONO);
   
    queryFPLEDG.readAll(FPLEDG, 1, 9999, lstFPLEDG);
    
    logger.debug("lstToBeApproved.size=" + lstToBeApproved.size());
    for (int i=0;i<lstToBeApproved.size();i++) {
      Map<String, String> record = (Map<String, String>) lstToBeApproved[i];
		  String divi = record.DIVI.trim();
		  String suno = record.SUNO.trim();
		  String sino = record.SINO.trim();
		  String inyr = record.INYR.trim();
		  logger.debug("call APS110MI SINO=" + sino); 
		  
		  def  params = [ "DIVI": divi, "SUNO": suno, "SINO": sino, "INYR": inyr]; 
      def callback = {
        Map<String, String> response ->
      }
      miCaller.call("APS110MI","ApproveInvoice", params, callback);
    }
  }
   
  /**
   * lstFPLEDG - Callback function to return FPLEDG records
   *
  */
  Closure<?> lstFPLEDG = { DBContainer FPLEDG ->
    String divi = FPLEDG.get("EPDIVI").toString().trim();
    String suno = FPLEDG.get("EPSUNO").toString().trim();
    String sino = FPLEDG.get("EPSINO").toString().trim();
    String inyr = FPLEDG.get("EPINYR").toString().trim();
    String aprv = FPLEDG.get("EPAPRV").toString().trim();
    
    allStatusesOk = true;
    DBAction queryFGINHE = database.table("FGINHE").index("00").selection("F4INS0").build();
    DBContainer FGINHE = queryFGINHE.getContainer();
    FGINHE.set("F4CONO", XXCONO);
    FGINHE.set("F4DIVI", divi);
    FGINHE.set("F4SUNO", suno);
    FGINHE.set("F4SINO", sino);
    FGINHE.set("F4INYR", inyr.toInteger());
    
    if (queryFGINHE.read(FGINHE)) {
      String ins0 = FGINHE.get("F4INS0").toString().trim();
      if (ins0 == "33334") {
        def map = [DIVI: divi, SUNO: suno, SINO: sino, INYR: inyr];
        lstToBeApproved.add(map);
      }
    }
    
  }
  
}
