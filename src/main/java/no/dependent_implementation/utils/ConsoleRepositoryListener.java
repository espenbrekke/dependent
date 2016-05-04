/*******************************************************************************
 * Copyright (c) 2010, 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package no.dependent_implementation.utils;


import no.dependent_implementation.OutputBouble;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;

/**
 * A simplistic repository listener that logs events to the console.
 */
public class ConsoleRepositoryListener
    extends AbstractRepositoryListener
{
    public ConsoleRepositoryListener(){
    }

    public void artifactDeployed( RepositoryEvent event )
    {
        OutputBouble.log3("Deployed " + event.getArtifact() + " to " + event.getRepository());
    }

    public void artifactDeploying( RepositoryEvent event )
    {
        OutputBouble.log3("Deploying " + event.getArtifact() + " to " + event.getRepository());
    }

    public void artifactDescriptorInvalid( RepositoryEvent event )
    {

        OutputBouble.reportError("Invalid artifact descriptor for " + event.getArtifact(), event.getException());
    }

    public void artifactDescriptorMissing( RepositoryEvent event )
    {
        OutputBouble.log3("Missing artifact descriptor for " + event.getArtifact());
    }

    public void artifactInstalled( RepositoryEvent event )
    {
        OutputBouble.log2("Installed " + event.getArtifact() + " to " + event.getFile());
    }

    public void artifactInstalling( RepositoryEvent event )
    {
        OutputBouble.log3("Installing " + event.getArtifact() + " to " + event.getFile());
    }

    public void artifactResolved( RepositoryEvent event )
    {
        OutputBouble.log3("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    public void artifactDownloading( RepositoryEvent event )
    {
        OutputBouble.log3("Downloading artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    public void artifactDownloaded( RepositoryEvent event )
    {
        OutputBouble.log3("Downloaded artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    public void artifactResolving( RepositoryEvent event )
    {
        OutputBouble.log3("Resolving artifact " + event.getArtifact());
    }

    public void metadataDeployed( RepositoryEvent event )
    {
        OutputBouble.log3("Deployed " + event.getMetadata() + " to " + event.getRepository());
    }

    public void metadataDeploying( RepositoryEvent event )
    {
        OutputBouble.log3("Deploying " + event.getMetadata() + " to " + event.getRepository());
    }

    public void metadataInstalled( RepositoryEvent event )
    {
        OutputBouble.log2("Installed " + event.getMetadata() + " to " + event.getFile());
    }

    public void metadataInstalling( RepositoryEvent event )
    {
        OutputBouble.log3("Installing " + event.getMetadata() + " to " + event.getFile());
    }

    public void metadataInvalid( RepositoryEvent event )
    {
        OutputBouble.log3("Invalid metadata " + event.getMetadata());
    }

    public void metadataResolved( RepositoryEvent event )
    {
        OutputBouble.log3("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
    }

    public void metadataResolving( RepositoryEvent event )
    {
        OutputBouble.log3("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
    }

}
