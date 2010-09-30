/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.shrinkwrap.dependencies.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipFile;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.dependencies.DependencyBuilder;
import org.jboss.shrinkwrap.dependencies.DependencyRepository;
import org.jboss.shrinkwrap.dependencies.RepositorySettings;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.Exclusion;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * 
 */
public class MavenDependencies implements DependencyBuilder
{

   private RepositorySettings settings;

   private DependencyRepository repository;

   private List<Dependency> dependencies;

   private Artifact lastArtifact;

   private List<Exclusion> lastExclusions;

   private String lastScope;

   private boolean lastOptional;

   private static final Archive<?>[] ARCHIVE_CAST = new Archive<?>[0];

   public MavenDependencies()
   {
      this.settings = new MavenRepositorySettings();
      this.repository = new MavenDependencyRepository();
      this.dependencies = new ArrayList<Dependency>();
      resetLast();
   }

   public MavenDependencies configureFrom(String path)
   {
      return this;
   }

   public MavenDependencies loadPom(String path)
   {
      return this;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.shrinkwrap.dependencies.DependencyBuilder#artifact(java.lang.String)
    */
   public DependencyBuilder artifact(String coordinates)
   {
      // add as an dependency
      if (lastArtifact != null)
      {
         addLastAsDependency();
         resetLast();
      }

      this.lastArtifact = new DefaultArtifact(coordinates);
      return this;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.shrinkwrap.dependencies.DependencyBuilder#exclusion(org.sonatype.aether.graph.Exclusion)
    */
   public DependencyBuilder exclusion(Exclusion exclusion)
   {
      this.lastExclusions.add(exclusion);
      return this;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.shrinkwrap.dependencies.DependencyBuilder#exclusions(org.sonatype.aether.graph.Exclusion[])
    */
   public DependencyBuilder exclusions(Exclusion... exclusions)
   {
      this.lastExclusions.addAll(Arrays.asList(exclusions));
      return this;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.shrinkwrap.dependencies.DependencyBuilder#exclusions(java.util.Collection)
    */
   public DependencyBuilder exclusions(Collection<Exclusion> exclusions)
   {
      this.lastExclusions.addAll(exclusions);
      return this;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.shrinkwrap.dependencies.DependencyBuilder#optional(boolean)
    */
   public DependencyBuilder optional(boolean optional)
   {
      this.lastOptional = optional;
      return this;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.shrinkwrap.dependencies.DependencyBuilder#scope(java.lang.String)
    */
   public DependencyBuilder scope(String scope)
   {
      this.lastScope = scope;
      return this;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.shrinkwrap.dependencies.DependencyBuilder#resolve()
    */
   public Archive<?>[] resolve() throws Exception
   {
      if (lastArtifact != null)
      {
         addLastAsDependency();
      }

      Validate.notEmpty(dependencies, "No dependencies were set to be resolved");

      RepositorySystem system = repository.getRepositorySystem();
      RepositorySystemSession session = repository.getSession(system, settings);

      CollectRequest request = new CollectRequest(dependencies, null, settings.getRemoteRepositories());

      List<ArtifactResult> artifacts = system.resolveDependencies(session, request, null);

      Collection<Archive<?>> archives = new ArrayList<Archive<?>>(artifacts.size());

      for (ArtifactResult artifact : artifacts)
      {
         File file = artifact.getArtifact().getFile();
         Archive<?> archive = ShrinkWrap.create(JavaArchive.class, file.getName()).as(ZipImporter.class).importFrom(new ZipFile(file)).as(JavaArchive.class);

         archives.add(archive);
      }

      return archives.toArray(ARCHIVE_CAST);

   }

   private void resetLast()
   {
      this.lastExclusions = new ArrayList<Exclusion>();
      this.lastScope = "";
      this.lastOptional = false;
      this.lastArtifact = null;
   }

   private void addLastAsDependency()
   {
      Dependency dependency = new Dependency(lastArtifact, lastScope, lastOptional, lastExclusions);
      dependencies.add(dependency);
   }

}
