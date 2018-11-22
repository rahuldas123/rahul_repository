package com.pgirop.composer;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;

import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.pgirop.Constants.HealthRecordConstants;
import com.pgirop.DB.DBComponent;
import com.pgirop.DB.SavePatientDetail;
import com.pgirop.exceptions.DBComponentException;
import com.pgirop.exceptions.HandleException;
import com.pgirop.pojo.PgiPatient;

public class AddPatientComposer extends SelectorComposer<Window>  {

	private static final long serialVersionUID = 1L;

	@Wire
	private Datebox dob , screenDate;

	@Wire
	private Textbox babyCr , motherName , email;

	@Wire
	private Radiogroup birthGroup;

	@Wire
	private Doublebox weight;

	@Wire
	private Textbox phoneNo , secondPhoneNo ,outbornText;

	@Wire
	private Button submitBtn;

	@Wire
	private Combobox enrollPlace , language , gestationAge ,gestationDays;

	@Wire
	private Label if_outborn;

	//private java.util.Date dateofAdmission = new java.util.Date();
	/*String cr = "";
	String pName = "";
	String pNumber = "";*/
	private boolean crFlag = true;
	//private String loggedUserSchemaId = ((String) Executions.getCurrent().getSession().getAttribute("companySchemaId"));
	private String loggedUserSchemaId = "demo";
	// user.

	public void doAfterCompose(Window comp) {

		try {
			super.doAfterCompose(comp);
			if (loggedUserSchemaId != null) {
				outbornText.setVisible(false);
				if_outborn.setVisible(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Listen("onChange=textbox#phoneNo")
	public void phoneNoLengthCheck() {
		if (this.phoneNo.getText().length() < 10) {
			Messagebox.show("Please enter atleast 10 digits !");
		}
	}

	@Listen("onChange=doublebox#weight")
	public void checkBirthWeightg() {
		if (weight.getValue() > 5000) {
			weight.setValue(0);
			Messagebox.show("Birth weight can't be more than 5000 g, Enter Value in grams only");
		}
	}

	@Listen("onCheck=radiogroup#birthGroup")
	public void onCheckBirthPlace(){
		if (this.birthGroup.getSelectedItem().getLabel().equals("Inborn")) {
			outbornText.setVisible(false);
			if_outborn.setVisible(false);
		}
		if (this.birthGroup.getSelectedItem().getLabel().equals("Outborn")) {
			outbornText.setVisible(true);
			if_outborn.setVisible(true);
		}
	}

	@SuppressWarnings("unchecked")
	@Listen("onChange=textbox#babyCr")
	public void uniqueIdCheck() {
		String babycr = babyCr.getText();
		String query = "SELECT babycr FROM pgihospital." + loggedUserSchemaId + ".patient where babycr='"
				+ babycr + "'";
		List<Object> queryList = null;
		try {
			queryList = DBComponent.getListFromNativeQuery(query);
		} catch (DBComponentException e) {
			e.printStackTrace();
		}
		if (queryList.isEmpty()) {
			crFlag = true;
		} else {
			crFlag = false;
			Messagebox.show("CRNumber " + babycr + " Aleady Exist ");
		}
	}

	@Listen("onChange=combobox#gestationAge")
	public void checkGestationWeek(){

		if (this.gestationAge.getSelectedItem() == null ) {
			Messagebox.show("Please select Gestation Age(Weeks) ");
			return;
		}else{
			calculateScreenDate();
		}
	}
	
	public void calculateScreenDate(){

		Date a_screendate = null;
		java.util.Date dob_e = dob.getValue();
		java.sql.Date sDobDate_e = convertUtilToSql(dob_e);
		Integer gestationWeeks_e = 
				Integer.parseInt(gestationAge.getSelectedItem().getLabel());
		double weight_e = weight.getValue();

		System.out.println(sDobDate_e);
		System.out.println(gestationWeeks_e+" "+weight_e);

		a_screendate = getScreenDate(sDobDate_e ,weight_e , gestationWeeks_e);	

		screenDate.setValue(a_screendate);



	}
	public static Date getScreenDate(Date dob,Double wt,int gest){
		Date sdate = null;
		Date _dob = dob;
		Calendar c = Calendar.getInstance();
		c.setTime(_dob);
		if(gest<28 || wt<1200){
			c.add(Calendar.DAY_OF_MONTH, 21);
		}else if(gest<34 || wt<2000){
			c.add(Calendar.DAY_OF_MONTH, 28);
		}else {
			c.add(Calendar.DAY_OF_MONTH, 28);
		}
		int dayno = c.get(Calendar.DAY_OF_WEEK);
		if(!((dayno == 2) || (dayno == 4) || (dayno == 6))){
			int val = 0;
			if(dayno == 1)
				val = 2;
			else
				val = 1;
			c.add(Calendar.DAY_OF_MONTH, (-val));
		}
		sdate = c.getTime();
		return sdate;
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Listen("onClick = #submitBtn")
	public void insertRow() throws Exception {

		if (weight.getValue() <= 0) {
			// Weight can't be 0
			Messagebox.show("Enter Birth Weight");
			return;
		}
		if (this.gestationAge.getSelectedItem() == null ) {
			// gAge range
			Messagebox.show("Please select Gestation Age(Weeks) ");
			return;
		}
		if (this.gestationDays.getSelectedItem() == null ) {
			// gAge range
			Messagebox.show("Please select Gestation Age(Days) ");
			return;
		}


		if (!crFlag) { // CRNo. exist in DB
			Messagebox.show("CRNumber " + " Aleady Exist ");
			return;
		}
		if (this.motherName.getText().equals("") || this.motherName.getText().equals(null)) {
			Messagebox.show("Please enter name, it's a required field");
		} else if (this.phoneNo.getText().length() < 10) {
			Messagebox.show("Please enter atleast 10 digit phone number !");
		} else if (this.babyCr.getText().equals("") || this.babyCr.getText().equals(null)) {
			Messagebox.show("Please enter Cr no., it's a required field");
		} else {
			String name = this.motherName.getValue();

			java.util.Date uDobDate = dob.getValue();
			java.sql.Date sDobDate = convertUtilToSql(uDobDate);

			java.util.Date uScreenDate = null;
			java.sql.Date sScreenDate = null;

			if ( this.screenDate.getValue() != null ) {
				uScreenDate = screenDate.getValue();
				sScreenDate = convertUtilToSql(uScreenDate);
				addPatient(name, sDobDate, sScreenDate);
				
			} else {
				addPatient(name, sDobDate, sScreenDate);
			}

		}
	}

	private void addPatient(String name, java.sql.Date sDobDate, java.sql.Date sScreenDate) throws DBComponentException {

		String baby_cr = babyCr.getText();
		String e_mail = null ;
		if (this.email.getText().equals("") || this.email.getText().equals(null)) {
			e_mail = "";
		} else {
			e_mail = email.getText();
		}

		String birth_group = null;
		if (birthGroup.getSelectedIndex() < 0) {
			birth_group = "";
		} else {
			birth_group = birthGroup.getSelectedItem().getLabel();
		}
		String phone_no = phoneNo.getValue();

		String second_phone_no = null;
		if (this.secondPhoneNo.getText().equals("") || this.secondPhoneNo.getText().equals(null)) {
			second_phone_no = "";
		} else {
			second_phone_no = secondPhoneNo.getText();
		}
		Double weigh = weight.getValue();
		String gestation_age = gestationAge.getSelectedItem().getLabel();
		String gestation_day = gestationDays.getSelectedItem().getLabel();

		String _outborn_loc = "";

		if (!(birthGroup.getSelectedIndex() < 0)) {			
			if (birthGroup.getSelectedItem().getLabel().equals("Outborn")) {
				if (this.outbornText.getValue() != null) {
					_outborn_loc = outbornText.getValue();
				}
			}
		}
		String enroll_place = null;
		if (this.enrollPlace.getSelectedItem() != null ) {
			enroll_place = enrollPlace.getSelectedItem().getLabel();
		} else {
			enroll_place = "";
		}

		String lang = null;
		if (this.language.getSelectedItem() != null ) {
			lang = language.getSelectedItem().getLabel();
		} else {
			lang = "";
		}
		java.sql.Date dob_date = sDobDate;
		java.sql.Date screen_date = sScreenDate;

		System.out.println("display of patient..");
		System.out.println(baby_cr);
		System.out.println(e_mail);
		System.out.println(name);
		System.out.println(birth_group);
		System.out.println(phone_no);
		System.out.println(second_phone_no);
		System.out.println(weigh);
		System.out.println(gestation_age);
		System.out.println(gestation_day);
		System.out.println(_outborn_loc);
		System.out.println(enroll_place);
		System.out.println(lang);
		System.out.println(dob_date);
		System.out.println(screen_date);

		System.out.println("user during save of patient.." + loggedUserSchemaId);

		PgiPatient patient = new PgiPatient();
		patient.setBabyCr(baby_cr);
		patient.setBirthGroup(birth_group);
		patient.setDob(dob_date);
		patient.setEmail(e_mail);
		patient.setEnrollPlace(enroll_place);
		patient.setGestationAge(gestation_age);
		patient.setGestationDays(gestation_day);
		patient.setLanguage(lang);
		patient.setMotherName(name);
		patient.setPhoneNo(phone_no);
		patient.setScreenDate(screen_date);
		patient.setSecondPhoneNo(second_phone_no);
		patient.setOutbornLocation(_outborn_loc);
		patient.setWeight(weigh);

		SavePatientDetail.save(patient);

		/*String eStatus = "Complete";

		Object retVal = DBComponent.saveObject(patient, loggedUserSchemaId);
		if(!retVal.equals(null)){
			Sessions.getCurrent().setAttribute("estatus", eStatus);
			Executions.sendRedirect("list_of_patient.zul");
		}*/
	}
	private static java.sql.Date convertUtilToSql(java.util.Date uDate) {
		java.sql.Date sDate = new java.sql.Date(uDate.getTime());
		return sDate;
	}
}