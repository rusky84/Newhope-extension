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
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 
 
 /*
 *Modification area - M3
 *Nbr               Date      User id     Description
 *WKF009            20230614  XWZHAO      Add record to EXTVAR when FPLEDG is add and updated
 *
 */
 
/*
* - Write FPLEDG records to EXTVAR
*/
public class AddVarianceTrns extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  
  private String cono;
  private String divi;
  private String yea4;
  private String jrno;
  private String jsno;
  
  private int currentDate;
  private int currentTime;
  
  
  private int XXCONO;
  
  public AddVarianceTrns(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program) {
    this.mi = mi;
    this.database = database;
    this.miCaller = miCaller;
    this.logger = logger;
    this.program = program;
  }
  
  public void main() {
    //Fetch input fields from MI
    cono = mi.inData.get("CONO") == null ? '' : mi.inData.get("CONO").trim();
  	if (cono == "?") {
  	  cono = "";
  	} 
    divi = mi.inData.get("DIVI") == null ? '' : mi.inData.get("DIVI").trim();
  	if (divi == "?") {
  	  divi = "";
  	} 
  	yea4 = mi.inData.get("YEA4") == null ? '' : mi.inData.get("YEA4").trim();
  	if (yea4 == "?") {
  	  yea4 = "";
  	} 
  	jrno = mi.inData.get("JRNO") == null ? '' : mi.inData.get("JRNO").trim();
  	if (jrno == "?") {
  	  jrno = "";
  	}
  	jsno = mi.inData.get("JSNO") == null ? '' : mi.inData.get("JSNO").trim();
  	if (jsno == "?") {
  	  jsno = "";
  	}
  	//Validate input fields
    if (!cono.isEmpty()) {
			if (cono.isInteger()){
				XXCONO= cono.toInteger();
			} else {
				mi.error("Company " + cono + " is invalid");
				return;
			}
		} else {
			XXCONO= program.LDAZD.CONO;
		}
    if (divi.isEmpty()) {
      mi.error("Division must be entered");
      return;
    }
    if (yea4.isEmpty()) {
      mi.error("Year must be entered");
      return;
    }
    if (jrno.isEmpty()) {
      mi.error("Journal number must be entered");
      return;
    }
    if (jsno.isEmpty()) {
      mi.error("Journal sequence number must be entered");
      return;
    }
    DBAction queryFGLEDG = database.table("FGLEDG").index("00").selection("EGJRNO", "EGJSNO").build()
    DBContainer FGLEDG = queryFGLEDG.getContainer();
    FGLEDG.set("EGCONO", XXCONO);
    FGLEDG.set("EGDIVI", divi);
    FGLEDG.set("EGYEA4", yea4.toInteger());
    FGLEDG.set("EGJRNO", jrno.toInteger());
    FGLEDG.set("EGJSNO", jsno.toInteger());
    
    if (!queryFGLEDG.read(FGLEDG)) {
      mi.error("Journal number does not exist in FGLEDG.");
      return;
    }
    
    currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
  	currentTime = Integer.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
  	
    DBAction queryEXTVAR = database.table("EXTVAR").index("00").selection("EXPROC").build()
    DBContainer EXTVAR = queryEXTVAR.getContainer();
    EXTVAR.set("EXCONO", XXCONO);
    EXTVAR.set("EXDIVI", divi);
    EXTVAR.set("EXYEA4", yea4.toInteger());
    EXTVAR.set("EXJRNO", jrno.toInteger());
    EXTVAR.set("EXJSNO", jsno.toInteger());
    
    if (!queryEXTVAR.readLock(EXTVAR, updateEXTVAR)) {
      writeEXTVAR();
    }
  }
  
  /*
   * updateEXTVAR - Callback function to update EXTVAR table
   *
   */
  Closure updateEXTVAR = { LockedResult EXTVAR ->
    EXTVAR.set("EXPROC", 0);
    EXTVAR.set("EXLMDT", currentDate);
  	EXTVAR.set("EXCHNO", EXTVAR.get("EXCHNO").toString().toInteger() +1);
  	EXTVAR.set("EXCHID", program.getUser());
    EXTVAR.update();
  }
  
  /*
	 * writeEXTVAR - write record to EXTVAR
	 *
	 */
	def writeEXTVAR() {
	 
	  DBAction actionEXTVAR = database.table("EXTVAR").build();
  	DBContainer EXTVAR = actionEXTVAR.getContainer();
  	EXTVAR.set("EXCONO", XXCONO);
  	EXTVAR.set("EXDIVI", divi);
  	EXTVAR.set("EXYEA4", yea4.toInteger());
  	EXTVAR.set("EXJRNO", jrno.toInteger());
    EXTVAR.set("EXJSNO", jsno.toInteger());
    EXTVAR.set("EXPROC", 0);
  	EXTVAR.set("EXRGDT", currentDate);
  	EXTVAR.set("EXRGTM", currentTime);
  	EXTVAR.set("EXLMDT", currentDate);
  	EXTVAR.set("EXCHNO", 0);
  	EXTVAR.set("EXCHID", program.getUser());
  	actionEXTVAR.insert(EXTVAR, recordExists);
	}
	/*
   * recordExists - return record already exists error message to the MI
   *
   */
  Closure recordExists = {
	  mi.error("Record already exists");
  }
}
