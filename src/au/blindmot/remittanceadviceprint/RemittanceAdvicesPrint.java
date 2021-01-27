/******************************************************************************                                                                                                                                                                                                
 * Copyright (C) 2019 Blindmotion & Adaxa         							  *                                                                                                                                                                                                
 * This program is free software; you can redistribute it and/or modify it    *                                                                                                                                                                                                
 * under the terms version 2 of the GNU General Public License as published   *                                                                                                                                                                                                
 * by the Free Software Foundation. This program is distributed in the hope   *                                                                                                                                                                                                
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *                                                                                                                                                                                                
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *                                                                                                                                                                                                
 * See the GNU General Public License for more details.                       *                                                                                                                                                                                                
 * You should have received a copy of the GNU General Public License along    *                                                                                                                                                                                                
 * with this program; if not, write to the Free Software Foundation, Inc.,    *                                                                                                                                                                                                
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *                                                                                                                                                                                                
 *****************************************************************************/  

package au.blindmot.remittanceadviceprint;

import java.io.File;
import java.io.FileInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.webui.apps.AEnv;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MProcess;
import org.compiere.model.MQuery;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.MUserMail;
import org.compiere.model.PrintInfo;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.EMail;

/**
 * Print Remittance Advices on paper or send PDFs
 * 
 * @author Nikunj Idempiere port Phil Barnett
 *
 */
public class RemittanceAdvicesPrint extends SvrProcess {

	
	private boolean		ISdDebtorStatement = false;
	private boolean		isForcePrint = false;
	private int			p_R_MailText_ID = 0;
	private int			m_C_BPartner_ID = 0;
	private int			m_C_PaySelection_ID = 0;
	private int			m_C_PrintFormat_ID = 0;
	
	private final static String PROCESS_NAME = "Remittance Advice - Payment Batch";
	private final static String TABLE_NAME = "C_PaySelection_Check_V";
	private final static String REMITTANCE_EMAIL_FIELD = "Remittance_Advice_Contact_ID";
	private final static String STATEMENT_EMAIL_FIELD = "Statement_Contact_ID";
	
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare() {

		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;

			else if (name.equals("R_MailText_ID"))
				p_R_MailText_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BPartner_ID"))
				m_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("C_PaySelection_ID"))
				m_C_PaySelection_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_PrintFormat_ID"))
				m_C_PrintFormat_ID = para[i].getParameterAsInt();
			else if (name.equals("IsDebtorStatement"))
				ISdDebtorStatement = "Y".equals(para[i].getParameter());
			else if (name.equals("Force_Print"))
				isForcePrint = "Y".equals(para[i].getParameter());

			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}

	}

	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception
	 */
	protected String doIt() throws Exception {
		
			
		MMailText mText  = new MMailText(getCtx(), p_R_MailText_ID, get_TrxName());
	/*	if (mText.get_ID() != p_R_MailText_ID)
			throw new AdempiereUserError ("@NotFound@: @R_MailText_ID@ - " + p_R_MailText_ID);*/

		
		ArrayList<File> pdfList = new ArrayList<File>(); //list of pdf
		MProcess process=null;
		ProcessInfo pi=null;
		
		int processID = DB.getSQLValueEx(get_TrxName(), "SELECT AD_Process_ID FROM AD_Process WHERE Name=?", PROCESS_NAME);
		int tableID=MTable.getTable_ID(TABLE_NAME);
		if(processID != -1 || tableID != -1){
			process=new MProcess(getCtx(), processID, get_TrxName());
			pi=new ProcessInfo(process.getName(), processID, tableID, 0);
		}
		else{
			throw new AdempiereUserError ("Process or table not found for report.");
		}
		MPrintFormat format = new MPrintFormat(getCtx(), m_C_PrintFormat_ID, get_TrxName());
		PrintInfo info=new PrintInfo(pi);
		int count=0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean isPrinted=false;
		MPaySelectionCheck msc=null;
			
		StringBuffer sql = new StringBuffer ("SELECT C_Bpartner_ID,C_PaySelection_ID, C_PaySelectionCheck_ID ,C_Payment_DocumentNO  FROM C_PaySelection_Check_V WHERE ");
		sql.append ("C_PaySelection_ID=").append (m_C_PaySelection_ID);
		
		//if business partner selected
		if (m_C_BPartner_ID != 0) {		
			
			sql.append(" AND ");
			sql.append("C_BPartner_ID=").append(m_C_BPartner_ID);
		}
		try{
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			rs = pstmt.executeQuery();

			//No match of business partner and payment selection
			if (!rs.isBeforeFirst()) {
				DB.close(rs, pstmt);
				throw new AdempiereUserError("No match found for selected payment selection and business partner.");
			}

			while  (rs.next()) {
				
				int c_BPartner_ID= rs.getInt(1);
				int c_PaySelection_ID = rs.getInt(2);
				int m_C_PaySlectionCheck_ID=rs.getInt(3);
				int paymentDocumentNo=rs.getInt(4);
			
				msc=new  MPaySelectionCheck(getCtx(), m_C_PaySlectionCheck_ID, get_TrxName());
				if(!isForcePrint){
					isPrinted=msc.get_ValueAsBoolean("Remittance_Printed");
				}
				if (!isPrinted) {						
				
					MQuery query = new MQuery(TABLE_NAME);
					query.addRestriction("C_PaySelection_ID", MQuery.EQUAL,new Integer(c_PaySelection_ID));
					query.addRestriction("C_BPartner_ID", MQuery.EQUAL,new Integer(c_BPartner_ID));
				
					ReportEngine re = new ReportEngine(getCtx(), format, query, info);
			
					File pdf = re.getPDF();
					
					boolean sent = sendMail(mText, c_BPartner_ID, pdf);
					if (sent) {
						addLog("Email sent for Document:" + paymentDocumentNo);
						count++;
					}else{
						
						pdfList.add(pdf);
					}
					
					msc.set_ValueOfColumn("Remittance_Printed", true);
					msc.save();
				}
				else {
					addLog( "Already printed Document :" + paymentDocumentNo);
				}

			}
		} 
		catch (Exception e) {
			log.log(Level.SEVERE, "doIt - " + sql, e);
			throw new Exception(e);
		} 
		finally {
			DB.close(rs, pstmt);
		}

		if(pdfList.size()>0){
			
			File outFile = File.createTempFile("RemittanceAdvice", ".pdf");	
			if (pdfList.size() > 1) {
				AEnv.mergePdf(pdfList, outFile);
			} 
			else {
				outFile = pdfList.get(0);
			}
			
			//Adempiere code that was replaced
			/*ReportCtl.getReportViewerProvider().openViewer("Remittance Advice",
					new FileInputStream(outFile));*/
			
			RemZkReportViewerProvider.openViewer("Remittance Advice",
					new FileInputStream(outFile));

		}
		return  "@Emailed@=" + count + " - @Printed@=" + pdfList.size();
	}  // doIt
	
	
	/**
	 * Send mail to business partner with attachment
	 * 
	 * @param mText mail text 
	 * @param C_BPartner_ID business partner Id
	 * @param attachment file to be sent
	 * 
	 * @return true if email successfully sent .
	 */
	public boolean sendMail(MMailText mText,int C_BPartner_ID,File attachment) {
		boolean sent=false;
		int toUserId=0;
		MBPartner bPartner=new MBPartner(getCtx(), C_BPartner_ID, get_TrxName());
		if(ISdDebtorStatement)
			 toUserId=bPartner.get_ValueAsInt(STATEMENT_EMAIL_FIELD);
		else
			 toUserId=bPartner.get_ValueAsInt(REMITTANCE_EMAIL_FIELD);
		
		String emailId = null;
		if(toUserId >0){
			MUser toUser = MUser.get(getCtx(), toUserId);
			emailId = toUser.getEMail();
		}
		
		
		if(toUserId == 0 || emailId == null)			
				addLog(" @RequestActionEMailError@ " + "Email id not found"  + " for this " + "@C_BPartner_ID@");
		else {

			
			MClient client = MClient.get(getCtx());
			EMail email = client.createEMail(emailId, mText.getMailHeader(), null);
			if (!email.isValid()) {
				addLog(" @RequestActionEMailError@ Invalid EMail: " + emailId);
				
			} 
			else {

				mText.setBPartner(C_BPartner_ID); 
				String message = mText.getMailText(true);
				if (mText.isHtml())
					email.setMessageHTML(mText.getMailHeader(), message);
				else {
					email.setSubject(mText.getMailHeader());
					email.setMessageText(message);
				}

				log.fine(emailId + " - " + attachment);
				email.addAttachment(attachment);

				String msg = email.send();
				MUserMail um = new MUserMail(mText, toUserId , email);
				um.saveEx();
				if (msg.equals(EMail.SENT_OK)) {
					sent=true;
				}
				 else {
					addLog(" @RequestActionEMailError@ " + msg + " - " + emailId);
					
				}
			}
		}
		return sent;
	}  //  doMail
	
}  // RemittanceAdvicesPrint

