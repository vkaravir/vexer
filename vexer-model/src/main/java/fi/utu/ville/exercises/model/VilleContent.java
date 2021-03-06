package fi.utu.ville.exercises.model;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.VerticalLayout;
import fi.utu.ville.exercises.model.VilleUI;

public abstract class VilleContent extends VerticalLayout implements View {

	private static final long serialVersionUID = 1L;
	private VilleUI ui;

	public VilleContent(VilleUI ui) {
		init(ui);
	}

	public void init(VilleUI ui) {
		this.ui = ui; 
	}

	public abstract void doLayout();

	public abstract boolean isOkToExit();
	
	public DialogWithConfirm  getConfirmExitDialog() {
		return null;
	}

	public VilleUI getVilleUI() {
		return ui;
	}

	protected boolean changeContent(VilleContent content) {
		return ui.changeContent(content);
	}
	
	/**
	 * The return value must be unique and suitable as URI (no spacing etc.).
	 * @return Unique URI
	 */
	public abstract String getViewName();
	
	/* Comes from interface View */
	@Override
	public void enter(ViewChangeEvent event) {
		// TODO Auto-generated method stub
		
	}

}