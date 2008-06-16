package no.bekk.fitnesse.plugins.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import fitnesse.html.HtmlUtil;
import fitnesse.wiki.PageData;
import fitnesse.wikitext.WidgetBuilder;
import fitnesse.wikitext.widgets.ClasspathWidget;
import fitnesse.wikitext.widgets.LineBreakWidget;
import fitnesse.wikitext.widgets.ParentWidget;
import fitnesse.wikitext.widgets.TextIgnoringWidgetRoot;

public class PomWidget extends ClasspathWidget {

	static {
		PageData.classpathWidgetBuilder = new WidgetBuilder(new Class[] { ClasspathWidget.class, PomWidget.class });
	}

	public static final String REGEXP = "^!pom [^\r\n]*";
	private static final Pattern pattern = Pattern.compile("^!pom (.*)");

	private List<String> classpaths;

	private String pomFile;
	boolean generateText;

	private MavenEmbedder mavenEmbedder;
	MavenProject mavenProject;
	private Matcher matcher;
	private String repo;
	private boolean embedderStarted;

	public PomWidget(ParentWidget parent, String inputText) throws Exception {
		super(null, "");
		String[] inputs = inputText.split("@");
		System.out.println("The input was:: ["+ inputs[0] +"]["+inputs[1]+"]");
		matcher = pattern.matcher(inputs[0]);
		repo = inputs[1];
		mavenEmbedder = new MavenEmbedder();
		classpaths = new ArrayList<String>();
		parent.addChild(this);
		
		embedderStarted = startMavenEmbedder(mavenEmbedder);
		pomFile = findPomFile(matcher);
		mavenProject = readMavenProjectFromPom(mavenEmbedder, pomFile);
		findOutputDirs(mavenProject, classpaths);
		findArtifacts(mavenProject, classpaths, repo);
		embedderStarted = stopMavenEmbedder(mavenEmbedder, embedderStarted);
		insertLineBreak(parent);
		createClasspathWidgets(parent, classpaths);
	}
	
	public String render() throws Exception {
		return HtmlUtil.metaText("fitness-pom-widget worked it's magic on: " + pomFile);
	}

	static boolean startMavenEmbedder(MavenEmbedder mavenEmbedder) throws MavenEmbedderException {
		mavenEmbedder.setClassLoader(Thread.currentThread().getContextClassLoader());
		mavenEmbedder.start();
		return true;
	}

	static boolean stopMavenEmbedder(MavenEmbedder mavenEmbedder, boolean embedderStarted) throws MavenEmbedderException {
		if (embedderStarted) {
			mavenEmbedder.stop();
		}
		return false;
	}

	static String findPomFile(Matcher matcher) {
		if (matcher.find())
			return matcher.group(1);
		else
			return "";
	}
	
	static MavenProject readMavenProjectFromPom(MavenEmbedder mavenEmbedder, String pomFile) throws ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException {
		return mavenEmbedder.readProjectWithDependencies(new File(pomFile));
	}

	static void findOutputDirs(MavenProject mavenProject, List<String> classpaths) {
		Build mavenBuild = mavenProject.getBuild();
		if (mavenBuild != null) {
			String outputDir = mavenBuild.getOutputDirectory();
			String testOurputDir = mavenBuild.getTestOutputDirectory();
			classpaths.add(outputDir);
			classpaths.add(testOurputDir);
		}
	}

	@SuppressWarnings("unchecked")
	static void findArtifacts(MavenProject mavenProject, List<String> classpaths, String repo) throws Exception {
		Set<Artifact> artifacts = mavenProject.getArtifacts();
		if (artifacts != null) {
			for (Artifact artifact : artifacts) {
				File file = artifact.getFile();
				if (file != null) {
					String name = artifact.getFile().getName();
					String classpath = String.format("%s/%s/%s/%s/%s", repo , artifact.getGroupId().replaceAll("\\.", "/"), artifact.getArtifactId(), artifact.getVersion(), name);
					if(name.contains("fitnesse"))
						classpaths.add(0, classpath);
					else
						classpaths.add(classpath);
				}
			}
		}
	}
	
	static void createClasspathWidgets(ParentWidget parent, List<String> classpaths) throws Exception {
		for (String classpath : classpaths) {
			new ClasspathWidget(parent, String.format("%s %s", "!path", classpath));
			insertLineBreak(parent);
		}
	}

	static void insertLineBreak(ParentWidget parent) {
		if(!(parent instanceof TextIgnoringWidgetRoot))
			new LineBreakWidget(parent, "");
	}

	public static class Builder {
		private MavenEmbedder mavenEmbedder;
		private MavenProject mavenProject;
		private String inputText;
		private List<String> classpaths;
		private ParentWidget parent;

		public Builder(ParentWidget parent, String inputText) {
			this.parent = parent;
			this.inputText = inputText;
		}

		public Builder mavenEmbedder(MavenEmbedder mavenEmbedder) {
			this.mavenEmbedder = mavenEmbedder;
			return this;
		}

		public Builder mavenProject(MavenProject mavenProject) {
			this.mavenProject = mavenProject;
			return this;
		}
		
		public Builder classpaths(List<String> classpaths) {
			this.classpaths = classpaths;
			return this;
		}
		
		public PomWidget build() throws Exception {
			return new PomWidget(this);
		}
	}

	private PomWidget(Builder builder) throws Exception {
		super(builder.parent, builder.inputText);
		this.mavenEmbedder = builder.mavenEmbedder;
		this.mavenProject = builder.mavenProject;
		this.classpaths = builder.classpaths;
		this.matcher = pattern.matcher(builder.inputText);
	}
}
