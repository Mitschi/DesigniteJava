package Designite.SourceModel;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Document;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import Designite.InputArgs;
import Designite.utils.Logger;

public class SM_Project extends SM_SourceItem implements MetricsExtractable {

	private InputArgs inputArgs;
	private List<String> sourceFileList;
	private List<CompilationUnit> compilationUnitList;
	private List<SM_Package> packageList;
	private String unitName;

	// TODO set project name
	public SM_Project(InputArgs argsObj) {
		this.inputArgs = argsObj;
		sourceFileList = new ArrayList<String>();
		compilationUnitList = new ArrayList<CompilationUnit>();
		packageList = new ArrayList<SM_Package>();
		setName("Project");
	}

	public void setName(String name) {
		this.name = name;
	}

	/*
	 * public void setSourceFileList(List<String> list) { sourceFileList = list;
	 * }
	 */

	public List<String> getSourceFileList() {
		return sourceFileList;
	}

	public void setCompilationUnitList(List<CompilationUnit> list) {
		if(list == null)
			throw new NullPointerException();
		compilationUnitList = list;
	}

	public List<CompilationUnit> getCompilationUnitList() {
		return compilationUnitList;
	}

	public List<SM_Package> getPackageList() {
		return packageList;
	}

	public int getPackageCounter() {
		return packageList.size();
	}

	// method used in tests
	public CompilationUnit createCU(String filePath) {
		String fileToString = readFileToString(filePath);
		int startingIndex = filePath.lastIndexOf(File.separatorChar);
		unitName = filePath.substring(startingIndex + 1);

		return createAST(fileToString, unitName);
	}

	private void parseAllPackages() {
		for (SM_Package pkg : packageList) {
			pkg.parse();
		}
	}

	private void createPackageObjects() {
		checkNotNull(compilationUnitList);
		String packageName;
		for (CompilationUnit unit : compilationUnitList) {
			if (unit.getPackage() != null) {
				packageName = unit.getPackage().getName().toString();
			} else {
				packageName = "(default package)";
			}
			SM_Package pkgObj = searchPackage(packageName);
			// If pkgObj is null, package has not yet created
			if (pkgObj == null) {
				pkgObj = new SM_Package(packageName, this);
				packageList.add(pkgObj);
			}
			pkgObj.addCompilationUnit(unit);
		}
	}

	private void checkNotNull(List<CompilationUnit> list) {
		if (list == null) {
			Logger.log("Application couldn't find any source code files in the specified path.");
			System.exit(1);
		}
	}

	private SM_Package searchPackage(String packageName) {
		for (SM_Package pkg : packageList) {
			if (pkg.getName().equals(packageName))
				return pkg;
		}
		return null;
	}

	private void createCompilationUnits() {
		try {
			getFileList(inputArgs.getSourceFolder());

			for (String file : sourceFileList) {
				String fileToString = readFileToString(file);
				int startingIndex = file.lastIndexOf(File.separatorChar);
				unitName = file.substring(startingIndex + 1);
				CompilationUnit unit = createAST(fileToString, unitName);
				if (unit != null)
					compilationUnitList.add(unit);
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	private CompilationUnit createAST(final String content, String unitName) {
		Document doc = new Document(content);
		final ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);

		parser.setUnitName(unitName);
		// String [] sources =
		// {"/Users/Tushar/Documents/Workspace/DesigniteJava/src/"};
		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);
		String[] sources = { inputArgs.getSourceFolder() };
		parser.setEnvironment(null, sources, null, true);
		parser.setSource(doc.get().toCharArray());

		CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		 if (!cu.getAST().hasBindingsRecovery()) {
		 System.out.println("Binding not activated."); }
		 
		return cu;
	}

	private void getFileList(String path) {
		File root = new File(path);
		File[] list = root.listFiles();
		// List<String> sourceFileList = new ArrayList<String>();

		if (list == null)
			return;
		for (File f : list) {
			if (f.isDirectory()) {
				getFileList(f.getAbsolutePath());
			} else {

				if (f.getName().endsWith(".java"))
					sourceFileList.add(f.getAbsolutePath());
			}
		}
		return;
	}

	private String readFileToString(String sourcePath) {
		try {
			return new String(Files.readAllBytes(Paths.get(sourcePath)));
		} catch (IOException e) {
			e.printStackTrace();
			return new String();
		}
	}

	@Override
	public void printDebugLog(PrintWriter writer) {
		print(writer, "Project: " + name);
		print(writer, "-------------------");
		for (SM_Package pkg : packageList) {
			pkg.printDebugLog(writer);
		}
	}

	@Override
	public void parse() {
		Logger.log("Parsing the source code ...");
		createCompilationUnits();
		createPackageObjects();
		parseAllPackages();
	}

	public void resolve() {
		Logger.log("Resolving symbols...");
		for (SM_Package pkg : packageList) {
			pkg.resolve();
		}
	}
	
	@Override
	public void extractMetrics() {
		Logger.log("Extracting metrics...");
		for (SM_Package pkg : packageList) {
			pkg.extractMetrics();
		}
	}

}
