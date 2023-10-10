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
 *EXT019            20231005  RMURRAY     Added item type, planning date, updated field, validation
 */
 
/*
 * Add WO part
 */
public class AddWOPart extends ExtendM3Transaction {
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
  private String fuds;
  private String itds;
  private String stcd;
  private String popn;
  private String cnqt;
  private String aval;
  private String peun;
  private String ltyp;
  private String pupr;
  private String suno;
  private String whlo;
  private String opno;
  private String zzdt; 
  private String itty; 
  private String pop2;
  
  private int XXCONO;
  private int currentDate;
  private int currentTime;
  
  private int iLINO;
  private boolean isPOPNValid;
  
  public AddWOPart(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program) {
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
  	mtno = mi.inData.get("MTNO") == null ? '' : mi.inData.get("MTNO").trim();
  	if (mtno == "?") {
  	  mtno = "";
  	} 
  	cnqt = mi.inData.get("CNQT") == null ? '' : mi.inData.get("CNQT").trim();
  	if (cnqt == "?") {
  	  cnqt = "";
  	} 
  	ltyp = mi.inData.get("LTYP") == null ? '' : mi.inData.get("LTYP").trim();
  	if (ltyp == "?") {
  	  ltyp = "";
  	} 
  	suno = mi.inData.get("SUNO") == null ? '' : mi.inData.get("SUNO").trim();
  	if (suno == "?") {
  	  suno = "";
  	} 
 	whlo = mi.inData.get("WHLO") == null ? '' : mi.inData.get("WHLO").trim();
  	if (whlo == "?") {
  	  whlo = "";
  	} 
  	pupr = mi.inData.get("PUPR") == null ? '' : mi.inData.get("PUPR").trim();
  	if (pupr == "?") {
  	  pupr = "";
  	} 
  	itds = mi.inData.get("ITDS") == null ? '' : mi.inData.get("ITDS").trim();
  	if (itds == "?") {
  	  itds = "";
  	}
  	fuds = mi.inData.get("FUDS") == null ? '' : mi.inData.get("FUDS").trim();
  	if (fuds == "?") {
  	  fuds = "";
  	} 
  	opno = mi.inData.get("OPNO") == null ? '' : mi.inData.get("OPNO").trim();
  	if (opno == "?") {
  	  opno = "";
  	} 
  	zzdt = mi.inData.get("ZZDT") == null ? '' : mi.inData.get("ZZDT").trim();
  	if (zzdt == "?") {
  	  zzdt = "";
  	} 
  	itty = mi.inData.get("ITTY") == null ? '' : mi.inData.get("ITTY").trim();
  	if (itty == "?") {
  	  itty = "";
  	} 
  	pop2 = mi.inData.get("POP2") == null ? '' : mi.inData.get("POP2").trim();
  	if (pop2 == "?") {
  	  pop2 = "";
  	} 
    if (!validateInput()) {
      return;
    }
    
    ZoneId zid = ZoneId.of("Australia/Sydney"); 
    currentDate = LocalDate.now(zid).format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
    currentTime = Integer.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
    
    popn = "";
    DBAction queryMITPOP = database.table("MITPOP").index("00").selection("MPPOPN").build();
    DBContainer MITPOP = queryMITPOP.getContainer();
	MITPOP.set("MPCONO", XXCONO);
	MITPOP.set("MPALWT", 5);
	MITPOP.set("MPALWQ", "");
	MITPOP.set("MPITNO", mtno);
  	queryMITPOP.readAll(MITPOP, 4, 1, listMITPOP);
  	
  	String stqt = "";
  	String alqt = "";
  	aval = "";
  	DBAction queryMITBAL = database.table("MITBAL").index("00").selection("MBSTQT", "MBALQT").build();
    DBContainer MITBAL = queryMITBAL.getContainer();
    MITBAL.set("MBCONO", XXCONO);
    MITBAL.set("MBWHLO", whlo);
    MITBAL.set("MBITNO", mtno);
    if (queryMITBAL.read(MITBAL)) {
      stqt = MITBAL.get("MBSTQT").toString().trim();
      alqt = MITBAL.get("MBALQT").toString().trim();
      aval = (stqt.toDouble() - alqt.toDouble()).toString();
    }
  	
  	if (pupr.isEmpty()) {
      DBAction queryMITVEN = database.table("MITVEN").index("00").selection("IFPPUN", "IFPUPR").build();
      DBContainer MITVEN = queryMITVEN.getContainer();
      MITVEN.set("IFCONO", XXCONO);
      MITVEN.set("IFITNO", mtno);
      MITVEN.set("IFPRCS", "");
      MITVEN.set("IFSUFI", "");
      MITVEN.set("IFSUNO", suno);
      if (queryMITVEN.read(MITVEN)) {
        pupr = MITVEN.get("IFPUPR").toString().trim();
      }
  	}
    
    if (opno.isEmpty()) {
      DBAction queryMMOOPE = database.table("MMOOPE").index("00").selection("QOOPNO").build();
      DBContainer MMOOPE = queryMMOOPE.getContainer();
  	  MMOOPE.set("QOCONO", XXCONO);
  	  MMOOPE.set("QOFACI", faci);
  	  MMOOPE.set("QOPRNO", prno);
  	  MMOOPE.set("QOMWNO", mwno);
      queryMMOOPE.readAll(MMOOPE, 4, 1, listMMOOPE);
    }
  	
    // - Write to EXTMAT
    writeEXTMAT();
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
    if (lino == "") {
      iLINO = 0;
      DBAction queryEXTMAT = database.table("EXTMAT").index("00").selection("EXLINO").build();
      DBContainer EXTMAT = queryEXTMAT.getContainer();
  	  EXTMAT.set("EXCONO", XXCONO);
  	  EXTMAT.set("EXFACI", faci);
  	  EXTMAT.set("EXPRNO", prno);
  	  EXTMAT.set("EXMWNO", mwno);
  	  queryEXTMAT.readAll(EXTMAT, 4, listEXTMAT);
  	  lino = (iLINO + 1).toString();
    }
    if (mtno.isEmpty()){
  	  mi.error("Component number must be entered");
  	  return false;
  	}
  	DBAction queryMITMAS = database.table("MITMAS").index("00").selection("MMITDS", "MMFUDS", "MMUNMS", "MMSTCD", "MMITTY").build();
    DBContainer MITMAS = queryMITMAS.getContainer();
    MITMAS.set("MMCONO", XXCONO);
    MITMAS.set("MMITNO", mtno);
    if (!queryMITMAS.read(MITMAS)) {
      mi.error("Component number is invalid.");
      return false;
    }
    if (itds.isEmpty()) {
      itds = MITMAS.get("MMITDS").toString().trim();
    }
    if (fuds.isEmpty()) {
      fuds = MITMAS.get("MMFUDS").toString().trim();
    }
    stcd = MITMAS.get("MMSTCD").toString().trim();
    peun = MITMAS.get("MMUNMS").toString().trim();
    itty = MITMAS.get("MMITTY").toString().trim();
    if (cnqt.isEmpty()){
  	  mi.error("Quantity must be entered");
  	  return false;
  	}
  	if (ltyp.isEmpty()) {
  	  ltyp = "0";
  	} else {
  	  if (ltyp.toInteger() != 0 && ltyp.toInteger() != 1) {
  	    mi.error("Line type is invalid, can only be 0 or 1.");
        return false;
  	  }
  	}
  	if (!suno.isEmpty()) {
  	  DBAction queryCIDMAS = database.table("CIDMAS").index("00").selection("IDSUNO").build();
      DBContainer CIDMAS = queryCIDMAS.getContainer();
      CIDMAS.set("IDCONO", XXCONO);
      CIDMAS.set("IDSUNO", suno);
  	  if (!queryCIDMAS.read(CIDMAS)) {
        mi.error("Supplier is invalid.");
        return false;
      }
  	}
  	if (whlo.isEmpty()) {
  	  mi.error("Warehose must be entered");
  	  return false;
  	}
  	DBAction queryMITWHL = database.table("MITWHL").index("00").selection("MWWHNM").build();
    DBContainer MITWHL = queryMITWHL.getContainer();
    MITWHL.set("MWCONO", XXCONO);
    MITWHL.set("MWWHLO", whlo);
	  if (!queryMITWHL.read(MITWHL)) {
      mi.error("Warehouse is invalid.");
      return false;
    }
    if (!zzdt.isEmpty()) {
  	  if(zzdt.length() != 8 ){
  	    mi.error("Date length must be 8");
  	    return false;
  	  }
  	  DBAction queryCSYCAL = database.table("CSYCAL").index("00").selection("CDYMD8").build();
      DBContainer CSYCAL = queryCSYCAL.getContainer();
      CSYCAL.set("CDCONO", XXCONO);
      CSYCAL.set("CDYMD8", zzdt);
  	  if (!queryCSYCAL.read(CSYCAL)) {
        mi.error("Date is invalid. yyyyMMdd");
        return false;
      }
    }
    return true;
	}
	/*
	* listEXTMAT - Callback function to return EXTMAT
	*
	*/
	Closure<?> listEXTMAT = { DBContainer EXTMAT ->
		String lino_EXTMAT = EXTMAT.get("EXLINO").toString().trim();
		if (lino_EXTMAT.toInteger() > iLINO) {
			iLINO = lino_EXTMAT.toInteger();
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
	* listMMOOPE - Callback function to return MMOOPE
	*
	*/
	Closure<?> listMMOOPE = { DBContainer MMOOPE ->
		opno = MMOOPE.get("QOOPNO").toString().trim();
	}
	/*
	 * writeEXTMAT
	 *
	 */
	def writeEXTMAT() {
  	
		DBAction actionEXTMAT = database.table("EXTMAT").build();
		DBContainer EXTMAT = actionEXTMAT.getContainer();
		EXTMAT.set("EXCONO", XXCONO);
		EXTMAT.set("EXFACI", faci);
		EXTMAT.set("EXWHLO", whlo);
		EXTMAT.set("EXPRNO", prno);
		EXTMAT.set("EXMWNO", mwno);
		EXTMAT.set("EXLINO", lino.toInteger());
		EXTMAT.set("EXMTNO", mtno);
		EXTMAT.set("EXITDS", itds);
		EXTMAT.set("EXFUDS", fuds);
		EXTMAT.set("EXSTCD", stcd.toInteger());
		EXTMAT.set("EXPOPN", popn);
		EXTMAT.set("EXCNQT", cnqt.toDouble());
		if (!aval.isEmpty()) {
			EXTMAT.set("EXAVAL", aval.toDouble());
		}
		if (!peun.isEmpty()) {
			EXTMAT.set("EXPEUN", peun);
		}
		if (!pupr.isEmpty()) {
			EXTMAT.set("EXPUPR", pupr.toDouble());
		}
			EXTMAT.set("EXLTYP", ltyp.toInteger());
		if (!suno.isEmpty()) {
			EXTMAT.set("EXSUNO", suno);
		}
		if (!opno.isEmpty()) {
			EXTMAT.set("EXOPNO", opno.toInteger());
		}
		EXTMAT.set("EXRGDT", currentDate);
		EXTMAT.set("EXRGTM", currentTime);
		EXTMAT.set("EXLMDT", currentDate);
		EXTMAT.set("EXCHNO", 0);
		EXTMAT.set("EXCHID", program.getUser());
		if (!zzdt.isEmpty()) {
			EXTMAT.set("EXZZDT", zzdt.toInteger());
		} else {
			EXTMAT.set("EXZZDT", 0);
		}
		EXTMAT.set("EXITTY", itty);
		EXTMAT.set("EXPOP2", pop2);
		actionEXTMAT.insert(EXTMAT, recordExists);
	}
    /*
	* recordExists - return record already exists error message to the MI
	*/
	Closure recordExists = {
		mi.error("Record already exists");
	}
}