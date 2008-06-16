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


public class PomWidgetTest {
	
	@Mock ParentWidget parent;
	@Mock TextIgnoringWidgetRoot badParent;
	@Mock MavenEmbedder mavenEmbedder;
	@Mock MavenProject mavenProject;
	@Mock Build build;
	
	@BeforeMethod
	public void setUp() {
		initMocks(this);
	}
	
	@Test
	public void shouldSetClassLoaderOnAndStartMavenEmbedder() throws Exception {
		boolean embedderStarted = PomWidget.startMavenEmbedder(mavenEmbedder);
		verify(mavenEmbedder).setClassLoader(Thread.currentThread().getContextClassLoader());
		verify(mavenEmbedder).start();
		assertTrue(embedderStarted);
	}
	
	@Test
	public void shouldStopMavenEmbedder() throws Exception {
		PomWidget.startMavenEmbedder(mavenEmbedder);
		PomWidget.stopMavenEmbedder(mavenEmbedder, true);
		verify(mavenEmbedder).stop();
	}
	
	@Test
	public void shouldReadMavenProjectFromPom() throws Exception {
		String pom = "pom.xml";
		stub(mavenEmbedder.readProjectWithDependencies(new File(pom))).toReturn(mavenProject);
		
		PomWidget.readMavenProjectFromPom(mavenEmbedder, pom);
		
		verify(mavenEmbedder).readProjectWithDependencies(new File(pom));
	}
	
	@Test
	public void shouldAddOututDirsToClasspathArray() throws Exception {
		List<String> classpaths = new ArrayList<String>();
		stub(mavenProject.getBuild()).toReturn(build);
		stub(build.getOutputDirectory()).toReturn("output");
		stub(build.getTestOutputDirectory()).toReturn("testOutput");
		
		PomWidget.findOutputDirs(mavenProject, classpaths);
	
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
		stub(artifact.getGroupId()).toReturn("org.fitnesse");
		stub(artifact2.getGroupId()).toReturn("org.fitnesse");
		
		PomWidget.findArtifacts(mavenProject, classpaths, "m2repo");
		
		assertEquals(classpaths.size(), 2);
	}
}
