package fi.utu.ville.exercises.stub;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.reflections.Reflections;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

import fi.utu.ville.exercises.model.ExerciseTypeDescriptor;
import fi.utu.ville.standardutils.StandardUIFactory;
import fi.utu.ville.standardutils.ui.DynamicStyles;

/**
 * An abstract class that can be extended to create a simple Vaadin application
 * that can be used for testing and developing Ville-Exercise-Types
 * 
 * @author Riku Haavisto
 */
@Theme(value = "ville-theme")
@Title(value = "Ville exercise-type stub")
@StyleSheet({ "fonts/Lato.ttf", "fonts/fonts.css" })
public abstract class VilleExerStubUI extends UI {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6292932364056918285L;

	private static final Logger logger = Logger.getLogger(VilleExerStubUI.class
			.getName());

	private static final String LOCALES_TO_TEST_PARAM = "localesToTest";
	private static final String STUB_RES_PATH_PARAM = "stubResPath";
	private static final String TYPES_TO_LOAD_PARAM = "typesToLoad";

	@SuppressWarnings("rawtypes")
	private static final Class<? extends ExerciseTypeDescriptor> edTypeClass = ExerciseTypeDescriptor.class;

	@Override
	public void init(VaadinRequest req) {

		setStyleName("main-style");

		// parses required data (exer-types to test, locales to test,
		// resource-directory)
		// and then initiates the stub-UI

		List<ExerciseTypeDescriptor<?, ?>> typesToLoad = getTypesToLoad();
		List<Locale> localesToTest = getLocalesToTest();
		String stubResPath = getStubResourceBaseDir();

		if (typesToLoad.isEmpty() || stubResPath == null
				|| localesToTest.isEmpty()) {

			setContent(StandardUIFactory
					.getWarningPanel("Types-to-load and locales-to-test must be non-empty, and stub-base-resource-dir not null. <br/>"
							+ "Types-to-load: "
							+ typesToLoad
							+ "; locales-to-test: "
							+ localesToTest
							+ "; stub-res-path: " + stubResPath));

		} else {

			// add the stub-styles implementation for Dynamic-Styles
			DynamicStyles.registerDynamicStylesFactory(new StubStylesFactory());

			StubSessionData.initIfNeeded(typesToLoad, stubResPath,
					localesToTest);

			setContent(new StubStartView());
		}
	}

	/**
	 * Returns the list of {@link ExerciseTypeDescriptor}s describing the
	 * exercise-types that will be loaded for testing in the stub.
	 * 
	 * @return list of {@link ExerciseTypeDescriptor}s to be loaded for testing
	 */
	protected List<ExerciseTypeDescriptor<?, ?>> getTypesToLoad() {
		{

			Set<Class<?>> potentialClasses;
			List<ExerciseTypeDescriptor<?, ?>> res = new ArrayList<ExerciseTypeDescriptor<?, ?>>();

			String toLoadClasses = VaadinServlet.getCurrent()
					.getServletContext().getInitParameter(TYPES_TO_LOAD_PARAM);
			if (toLoadClasses != null && !toLoadClasses.equals("")) {
				potentialClasses = new HashSet<Class<?>>();

				String[] classes = toLoadClasses.split(";");
				for (String cls : classes) {
					try {
						Class<?> aCls = Class.forName(cls);
						potentialClasses.add(aCls);
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} else {
				potentialClasses = findPotentialClasses();
			}

			for (Class<?> potentialClass : potentialClasses) {
				ExerciseTypeDescriptor<?, ?> etDesc = parseDescFromCls(potentialClass);
				if (etDesc != null) {
					res.add(etDesc);
				}
			}

			return res;

		}
	}

	/**
	 * Method for trying to find {@link ExerciseTypeDescriptor}s by reflection
	 * if none are explicitly set to be loaded.
	 * 
	 * @return all {@link ExerciseTypeDescriptor}-instances that might represent
	 *         {@link ExerciseTypeDescriptor} that could be loaded for testing
	 *         in the stub
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Set<Class<?>> findPotentialClasses() {
		logger.info("No exer-types to load were explicitly set");
		logger.info("Trying to find exer-types from classpath with package fi.utu.ville.*");

		Reflections reflections = new Reflections("fi.utu.ville", "edu.vserver");

		Set<Class<? extends ExerciseTypeDescriptor>> subTypes = reflections
				.getSubTypesOf(ExerciseTypeDescriptor.class);

		return (Set) subTypes;
	}

	/**
	 * Instantiates an {@link ExerciseTypeDescriptor} from the given
	 * {@link Class} if that is possible (eg. class is not abstract helper
	 * class).
	 * 
	 * @param parseFrom
	 *            {@link Class}-file representing the class that will be parsed
	 * @return {@link ExerciseTypeDescriptor} parsed or null if one cannot be
	 *         parsed
	 */
	private static ExerciseTypeDescriptor<?, ?> parseDescFromCls(
			Class<?> parseFrom) {
		ExerciseTypeDescriptor<?, ?> res = null;

		// abstract-classes can only represent 'helpers'

		try {
			// first try to find a field containing type-descriptor as a
			// singleton-like instance

			Field[] clsFields = parseFrom.getFields();

			for (Field field : clsFields) {
				if (Modifier.isStatic(field.getModifiers())
						&& edTypeClass.isAssignableFrom(field.getType())) {
					res = edTypeClass.cast(field.get(null));
				}
			}

			// if no field is found try to instantiate the class itself (if not
			// abstract)

			if (res == null && !Modifier.isAbstract(parseFrom.getModifiers())
					&& edTypeClass.isAssignableFrom(parseFrom)) {
				res = edTypeClass.cast(parseFrom.newInstance());

			}

		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

	/**
	 * Returns path to directory where the stub should store it files.
	 * 
	 * @return path to the base-directory of stub-resources
	 */
	protected String getStubResourceBaseDir() {

		String pathToRes = VaadinServlet.getCurrent().getServletContext()
				.getInitParameter(STUB_RES_PATH_PARAM);

		if (pathToRes == null || pathToRes.equals("")) {
			pathToRes = VaadinServlet.getCurrent().getServletContext()
					.getRealPath("/VILLE/stub");
			// likely deployed as war, use temp-location
			if (pathToRes == null) {
				logger.warning("Placing VILLE-stub-resources-directory under temp");
				pathToRes = FileUtils.getTempDirectoryPath() + "/VILLE/stub";
			}
		}

		logger.info("Used VILLE-stub resource path: " + pathToRes);

		return pathToRes;
	}

	/**
	 * Return a list of Locales that will be available in the testing UI.
	 * 
	 * @return {@link List} of {@link Locale}s
	 */
	protected List<Locale> getLocalesToTest() {

		String localesStr = VaadinServlet.getCurrent().getServletContext()
				.getInitParameter(LOCALES_TO_TEST_PARAM);

		List<Locale> res = new ArrayList<Locale>();
		if (localesStr != null && !localesStr.equals("")) {
			String[] locales = localesStr.split(";");
			for (String aLocal : locales) {
				if (aLocal.contains("_")) {
					String[] langCountry = aLocal.split("_");
					res.add(new Locale(langCountry[0], langCountry[1]));
				} else {
					res.add(new Locale(aLocal));
				}
			}
		} else {

			res.add(Locale.ENGLISH);
		}
		return res;
	}
}