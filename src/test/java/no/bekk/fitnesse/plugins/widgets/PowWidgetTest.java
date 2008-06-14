package no.bekk.fitnesse.plugins.widgets;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.mockito.MockitoAnnotations.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fitnesse.wikitext.widgets.ParentWidget;
import fitnesse.wikitext.widgets.TextIgnoringWidgetRoot;


public class PowWidgetTest {
	
	@Mock ParentWidget parent;
	@Mock TextIgnoringWidgetRoot badParent;
	@Mock MavenEmbedder mavenEmbedder;
	@Mock MavenProject mavenProject;
	@Mock Build build;
	
	@BeforeMethod
	public void setUp() {
		initMocks(this);
	}
	
//	@Test
//	public void shoulNotGenerateTextIfParentIsInstanceOfTextIgnoringWidgetRoot() throws Exception{
//		PomWidget pomWidget = new PomWidget.Builder("").parent(badParent).build();
//		assertFalse(pomWidget.generateText);
//	}
//
//	@Test
//	public void shoulGenerateTextIfParentIsNotInstanceOfTextIgnoringWidgetRoot() throws Exception{
//		PomWidget pomWidget = new PomWidget.Builder("").parent(parent).build();
//		assertTrue(pomWidget.generateText);
//	}
//	
//	@Test
//	public void shoulNotAddThisAsChildIfGenerateTextFalse() throws Exception{
//		PomWidget pomWidget = new PomWidget.Builder("").parent(badParent).build();
//		pomWidget.insertLineBreak();
//		verifyNoMoreInteractions(badParent);
//	}
//	
//	@Test
//	public void shoulAddThisAsChildIfNotSetAndGenerateTextTrue() throws Exception{
//		ParentWidget parent = mock(ParentWidget.class);
//		PomWidget pomWidget = new PomWidget.Builder("").parent(parent).build();
//		pomWidget.insertLineBreak();
//		verify(parent).addChild(pomWidget);
//	}
	
	@Test
	public void shouldSetClassLoaderOnAndStartMavenEmbedder() throws Exception {
		PomWidget pomWidget = new PomWidget.Builder(null, "").mavenEmbedder(mavenEmbedder).build();
		pomWidget.startMavenEmbedder();
		verify(mavenEmbedder).setClassLoader(Thread.currentThread().getContextClassLoader());
		verify(mavenEmbedder).start();
		assertTrue(pomWidget.embedderStarted);
	}
	
	@Test
	public void shouldStopMavenEmbedder() throws Exception {
		PomWidget pomWidget = new PomWidget.Builder(null, "").mavenEmbedder(mavenEmbedder).build();
		pomWidget.startMavenEmbedder();
		pomWidget.stopMavenEmbedder();
		verify(mavenEmbedder).stop();
	}
	
	@Test
	public void shouldReadMavenProjectFromPom() throws Exception {
		String pom = "pom.xml";
		stub(mavenEmbedder.readProjectWithDependencies(new File(pom))).toReturn(mavenProject);
		
		PomWidget pomWidget = new PomWidget.Builder(null, "!pom pom.xml").mavenEmbedder(mavenEmbedder).build();
		pomWidget.readMavenProjectFromPom();
		
		verify(mavenEmbedder).readProjectWithDependencies(new File(pom));
		assertTrue(pomWidget.mavenProject != null);
	}
	
	@Test
	public void shouldAddOututDirsToClasspathArray() throws Exception {
		List<String> classpaths = new ArrayList<String>();
		stub(mavenProject.getBuild()).toReturn(build);
		stub(build.getOutputDirectory()).toReturn("output");
		stub(build.getTestOutputDirectory()).toReturn("testOutput");
		
		PomWidget pomWidget = new PomWidget.Builder(null, "").mavenProject(mavenProject).classpaths(classpaths).build();
		pomWidget.findOutputDirs();
	
		assertEquals(classpaths.size(), 2);
	}

	@Test
	public void shouldAddArtifactsToClasspathArray() throws Exception {
		List<String> classpaths = new ArrayList<String>();
		Set<Artifact> artifacts = new HashSet<Artifact>();
		Artifact artifact = mock(Artifact.class);
		Artifact artifact2 = mock(Artifact.class);
		artifacts.add(artifact);
		artifacts.add(artifact2);
		
		stub(mavenProject.getArtifacts()).toReturn(artifacts);
		stub(artifact.getFile()).toReturn(new File("path"));
		stub(artifact2.getFile()).toReturn(new File("path2"));
		
		PomWidget pomWidget = new PomWidget.Builder(null, "").mavenProject(mavenProject).classpaths(classpaths).build();
		pomWidget.findArtifacts();
		
		assertEquals(classpaths.size(), 2);
	}
	
	@Test
	public void shouldCreateClasspathWidgetsForAllElemetsInArray() throws Exception {
		List<String> classpaths = new ArrayList<String>();
		classpaths.add("classpath");
		PomWidget pomWidget = new PomWidget.Builder(parent, "").classpaths(classpaths).build();
		pomWidget.createClasspathWidgets();
		verify(parent).addChild(pomWidget);
		assertEquals(pomWidget.numberOfChildren(), 1);
	}
}
