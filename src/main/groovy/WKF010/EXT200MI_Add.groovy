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
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.text.DecimalFormat;


/*
 *Modification area - M3
 *Nbr               Date      User id     Description
 *ABF_R_200         20220405  RDRIESSEN   Mods BF0200- Write/Update EXTAPR records as a basis for PO authorization process
 *ABF_R_200         20220511  KVERCO      Update for XtendM3 review feedback
 *
 */

/**
* - Add Purchase Authorisation extension table row
*/
public class Add extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final MICallerAPI miCaller;
  private final LoggerAPI logger;
  private final ProgramAPI program;
  private final IonAPI ion;
  
  //Input fields
  private String puno;
  private String pnli;
  private String pnls;
  private String appr;
  private String asts;
  private String suno;
  private String orty;
  private String pupr;
  private String plpn;
  private String usid;
  private String orqa;
  private String lnam;
  private boolean found;
  

  private int XXCONO;
 
 /*
  * Add Purchase Authorisation extension table row
 */
  public Add(MIAPI mi, DatabaseAPI database, MICallerAPI miCaller, LoggerAPI logger, ProgramAPI program, IonAPI ion) {
    this.mi = mi;
    this.database = database;
  	this.miCaller = miCaller;
  	this.logger = logger;
  	this.program = program;
	  this.ion = ion;
	  
  }
  
  public void main() {
    
  	puno = mi.inData.get("PUNO") == null ? '' : mi.inData.get("PUNO").trim();
  	if (puno == "?") {
  	  puno = "";
  	} 
  	pnli = mi.inData.get("PNLI") == null ? '' : mi.inData.get("PNLI").trim();
  	if (pnli == "?") {
  	  pnli = "";
  	} 
  	pnls = mi.inData.get("PNLS") == null ? '' : mi.inData.get("PNLS").trim();
  	if (pnls == "?") {
  	  pnls = "";
  	} 
  	appr = mi.inData.get("APPR") == null ? '' : mi.inData.get("APPR").trim();
  	if (appr == "?") {
  	  appr = "";
  	} 
  	asts = mi.inData.get("ASTS") == null ? '' : mi.inData.get("ASTS").trim();
  	if (asts == "?") {
  	  asts = "";
  	} 
  	suno = mi.inData.get("SUNO") == null ? '' : mi.inData.get("SUNO").trim();
  	if (suno == "?") {
  	  suno = "";
  	}
  	orty = mi.inData.get("ORTY") == null ? '' : mi.inData.get("ORTY").trim();
  	if (orty == "?") {
  	  orty = "";
  	}
  	pupr = mi.inData.get("PUPR") == null ? '' : mi.inData.get("PUPR").trim();
  	if (pupr == "?") {
  	  pupr = "0";
  	}
  	usid = mi.inData.get("USID") == null ? '' : mi.inData.get("USID").trim();
  	if (usid == "?") {
  	  usid = "";
  	}
  	orqa = mi.inData.get("ORQA") == null ? '' : mi.inData.get("ORQA").trim();
  	if (orqa == "?") {
  	  orqa = "0";
  	}
  	lnam = mi.inData.get("LNAM") == null ? '' : mi.inData.get("LNAM").trim();
  	if (lnam == "?") {
  	  lnam = "0";
  	}
  	
		XXCONO = (Integer)program.LDAZD.CONO;

    // Validate input fields  	
		if (puno.isEmpty()) {
      mi.error("Purchase Order must be entered");
      return;
    }
    if (asts.isEmpty()) {
      mi.error("Authorisation status must be entered");
      return;
    }
    
    
    
    // - validate puno
    if (!puno.isEmpty()) {
    DBAction queryMPLINE = database.table("MPLINE").index("00").selection("IBPUNO", "IBPNLI", "IBPNLS").build();
    DBContainer MPLINE = queryMPLINE.getContainer();
    MPLINE.set("IBCONO", XXCONO);
    MPLINE.set("IBPUNO", puno);
    MPLINE.set("IBPNLI", pnli.toInteger());
    MPLINE.set("IBPNLS", pnls.toInteger());
    if (!queryMPLINE.read(MPLINE)) {
    
  	  found = false;
      DBAction queryMPOPLP = database.table("MPOPLP").index("00").selection("POCONO", "POPLPN", "POPLPS", "POPLP2").build();
      DBContainer MPOPLP = queryMPOPLP.getContainer();
      MPOPLP.set("POCONO", XXCONO);
      MPOPLP.set("POPLPN", puno.toInteger());
      MPOPLP.set("POPLPS", pnli.toInteger());
      MPOPLP.set("POPLP2", pnls.toInteger());
      queryMPOPLP.readAll(MPOPLP, 2, 1, lstMPOPLP);
      //queryMPOPLP.read(MPOPLP);
      if (!found) {
        mi.error("PO line number or Proposal number invalid");
        return;
      }
    }
  }
  
    if (!asts.equals("Authorised") && !asts.equals("Cancelled") && !asts.equals("Declined") && !asts.equals("Sent for approval") && !asts.equals("Cancelling workflow") && !asts.equals("Under authorisation")) {
      mi.error("Invalid authorisation status");
      return;
    }
    // - validate approver
    if (!appr.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", appr);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("Approver is invalid.");
        return;
      }
    }
    // - validate user
    if (!usid.isEmpty()) {
      DBAction queryCMNUSR = database.table("CMNUSR").index("00").selection("JUUSID").build();
      DBContainer CMNUSR = queryCMNUSR.getContainer();
      CMNUSR.set("JUCONO", 0);
      CMNUSR.set("JUDIVI", "");
      CMNUSR.set("JUUSID", usid);
      if (!queryCMNUSR.read(CMNUSR)) {
        mi.error("User is invalid.");
        return;
      }
    }
    // - validate supplier
    if (!suno.isEmpty()) {
      DBAction queryCIDMAS = database.table("CIDMAS").index("00").selection("IDSUNO").build();
      DBContainer CIDMAS = queryCIDMAS.getContainer();
      CIDMAS.set("IDCONO", XXCONO);
      CIDMAS.set("IDSUNO", suno);
      if (!queryCIDMAS.read(CIDMAS)) {
        mi.error("Supplier is invalid.");
        return;
      }
    }
    // - validate order type
    if (!orty.isEmpty()) {
      DBAction queryMPORDT = database.table("MPORDT").index("00").selection("OTORTY").build();
      DBContainer MPORDT = queryMPORDT.getContainer();
      MPORDT.set("OTCONO", XXCONO);
      MPORDT.set("OTORTY", orty);
      if (!queryMPORDT.read(MPORDT)) {
        mi.error("Order type is invalid.");
        return;
      }
    }
    writeEXTAPR(puno, appr, asts);
  }
  /**
  * Write Purchase Authorisation extension table EXTAPR
  *
  */
  def writeEXTAPR(String puno, String appr, String asts) {
	  //Current date and time
  	int currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger();
  	int currentTime = Integer.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
  	
	  DBAction actionEXTAPR = database.table("EXTAPR").build();
  	DBContainer EXTAPR = actionEXTAPR.getContainer();
  	EXTAPR.set("EXCONO", XXCONO);
  	EXTAPR.set("EXPUNO", puno);
  	EXTAPR.set("EXPNLI", pnli.toInteger());
  	EXTAPR.set("EXPNLS", pnls.toInteger());
  	EXTAPR.set("EXAPPR", appr);
  	EXTAPR.set("EXASTS", asts);
  	EXTAPR.set("EXSUNO", suno);
  	EXTAPR.set("EXORTY", orty);
  	EXTAPR.set("EXPUPR", pupr.toDouble());
  	EXTAPR.set("EXRGDT", currentDate);
  	EXTAPR.set("EXRGTM", currentTime);
  	EXTAPR.set("EXLMDT", currentDate);
  	EXTAPR.set("EXCHNO", 0);
  	EXTAPR.set("EXCHID", program.getUser());
  	EXTAPR.set("EXUSID", usid);
  	EXTAPR.set("EXORQA", orqa.toDouble());
  	EXTAPR.set("EXLNAM", lnam.toDouble());
  	actionEXTAPR.insert(EXTAPR, recordExists);
	}
  /**
   * recordExists - return record already exists error message to the MI
   *
  */
  Closure recordExists = {
	  mi.error("Record already exists");
  }
  
   Closure<?> lstMPOPLP = { DBContainer MPOPLP ->
    found = true;
  }
  
  
}
