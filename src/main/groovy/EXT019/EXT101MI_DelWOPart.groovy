
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
 
 
 /*
 *Modification area - M3
 *Nbr               Date      User id     Description
 *EXT019            20230322  WZHAO       Work Order Integrated with Parts Portal 
 *
 */
 
  /*
  * Delete WO part
 */
public class DelWOPart extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  
  private String cono;
  private String faci;
  private String prno;
  private String mwno;
  private String lino;
  private String mtno;
  
  private int XXCONO;
  private int currentDate;
  private int currentTime
  
  
  public DelWOPart(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program) {
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
    faci = mi.inData.get("FACI") == null ? '' : mi.inData.get("FACI").trim();
  	if (faci == "?") {
  	  faci = "";
  	} 
  	prno = mi.inData.get("PRNO") == null ? '' : mi.inData.get("PRNO").trim();
  	if (prno == "?") {
  	  prno = "";
  	} 
  	mwno = mi.inData.get("MWNO") == null ? '' : mi.inData.get("MWNO").trim();
  	if (mwno == "?") {
  	  mwno = "";
  	} 
  	lino = mi.inData.get("LINO") == null ? '' : mi.inData.get("LINO").trim();
  	if (lino == "?") {
  	  lino = "";
  	} 
  	
    if (!validateInput()) {
      return;
    }
    
    DBAction actionEXTMAT = database.table("EXTMAT").index("00").build();
    DBContainer EXTMAT = actionEXTMAT.getContainer();
    EXTMAT.set("EXCONO", XXCONO);
    EXTMAT.set("EXFACI", faci);
    EXTMAT.set("EXPRNO", prno);
    EXTMAT.set("EXMWNO", mwno);
    EXTMAT.set("EXLINO", lino.toInteger());
    if (!actionEXTMAT.readLock(EXTMAT, deleteEXTMAT)){
      mi.error("Record does not exists");
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
  
  	if (faci.isEmpty()){
  	  mi.error("Facility must be entered");
  	  return false;
  	}
  	DBAction queryCFACIL = database.table("CFACIL").index("00").selection("CFFACN").build();
    DBContainer CFACIL = queryCFACIL.getContainer();
    CFACIL.set("CFCONO", XXCONO);
    CFACIL.set("CFFACI", faci);
    if (!queryCFACIL.read(CFACIL)) {
      mi.error("Facility is invalid.");
      return false;
    }
    
    if (prno.isEmpty()){
  	  mi.error("Product must be entered");
  	  return false;
  	}
  	if (mwno.isEmpty()){
  	  mi.error("Work order must be entered");
  	  return false;
  	}
  	DBAction queryMMOHED = database.table("MMOHED").index("00").selection("QHWHST").build();
    DBContainer MMOHED = queryMMOHED.getContainer();
    MMOHED.set("QHCONO", XXCONO);
    MMOHED.set("QHFACI", faci);
    MMOHED.set("QHPRNO", prno);
    MMOHED.set("QHMWNO", mwno);
    if (!queryMMOHED.read(MMOHED)) {
      mi.error("WO no doesnot exist in MMOHED.");
      return false;
    }
    return true;
  }
  /*
   * deleteEXTMAT - Callback function to delete EXTMAT table
   *
   */
  Closure deleteEXTMAT = { LockedResult EXTMAT ->
    EXTMAT.delete();
  }
}
