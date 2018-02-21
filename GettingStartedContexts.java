/**
 * Copyright 2016 University of Zurich
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package examples;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.google.common.collect.Sets;

import cc.kave.commons.model.events.completionevents.Context;
import cc.kave.commons.model.naming.Names;
import cc.kave.commons.model.naming.codeelements.IEventName;
import cc.kave.commons.model.naming.codeelements.IMemberName;
import cc.kave.commons.model.naming.codeelements.IMethodName;
import cc.kave.commons.model.naming.codeelements.IParameterName;
import cc.kave.commons.model.naming.codeelements.IPropertyName;
import cc.kave.commons.model.naming.idecomponents.IProjectItemName;
import cc.kave.commons.model.naming.types.ITypeName;
import cc.kave.commons.model.ssts.IMemberDeclaration;
import cc.kave.commons.model.ssts.ISST;
import cc.kave.commons.model.ssts.IStatement;
import cc.kave.commons.model.ssts.blocks.IForEachLoop;
import cc.kave.commons.model.ssts.declarations.IDelegateDeclaration;
import cc.kave.commons.model.ssts.declarations.IEventDeclaration;
import cc.kave.commons.model.ssts.declarations.IFieldDeclaration;
import cc.kave.commons.model.ssts.declarations.IMethodDeclaration;
import cc.kave.commons.model.ssts.declarations.IPropertyDeclaration;
import cc.kave.commons.model.ssts.references.IMethodReference;
import cc.kave.commons.model.ssts.visitor.ISSTNode;
import cc.kave.commons.model.ssts.visitor.ISSTNodeVisitor;
import cc.kave.commons.model.typeshapes.IMemberHierarchy;
import cc.kave.commons.model.typeshapes.ITypeHierarchy;
import cc.kave.commons.model.typeshapes.ITypeShape;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;

public class GettingStartedContexts {

	private String ctxsDir;
	

	private Set<String> editKeywords = new HashSet<>();
	
	
	
	
	private Set<String> eventsKeywords = new HashSet<>();
	
	private CSVPrinter  editPrinter = null, eventsPrinter= null,allEventsPrinter = null;
	
	public GettingStartedContexts(String ctxsDir) {
		this.ctxsDir = ctxsDir;
	}

	public void run() throws IOException {
		
		String editFile = "edit.csv";
		String eventsFile = "events.csv";
		String allEventsFile = "all.csv";
		
		
		
		Writer  eventsWr, editWr, allEventsWr;
	
		try {
			editWr = new FileWriter(editFile);
			editPrinter = CSVFormat.DEFAULT.withHeader("Number_of_Events"," hasEditEvent", "EventNames", "Number_of_Methods", "hasEditMethod"," MethodNames").print(editWr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			eventsWr = new FileWriter(eventsFile);
			eventsPrinter = CSVFormat.DEFAULT.withHeader("Number_of_Events", "hasEventsEvent", "EventNames","Number_of_Methods", "hasEventsMethod"," MethodNames").print(eventsWr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			allEventsWr = new FileWriter(allEventsFile);
			allEventsPrinter = CSVFormat.DEFAULT.withHeader("Number_of_Events","EventNames", 
					"hasEditEvent","hasEditMethod", "hasEventsEvent", 
					"hasEventsMethod", "Number_of_Methods","MethodNames").print(allEventsWr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		editKeywords.add("cut");
		editKeywords.add("copy");
		editKeywords.add("paste");
		
		
		eventsKeywords.add("Navigation");
		eventsKeywords.add("completion");
		eventsKeywords.add("ide");
		eventsKeywords.add("Event");
		
		
		
		System.out.printf("looking (recursively) for solution zips in folder %s\n",
				new File(ctxsDir).getAbsolutePath());

		/*
		 * Each .zip that is contained in the ctxsDir represents all contexts that we
		 * have extracted for a specific C# solution found on GitHub. The folder
		 * represents the structure of the repository. The first level is the GitHub
		 * user, the second the repository name. After that, the folder structure
		 * represents the file organization within the respective repository. If you
		 * manually open the corresponding GitHub repository, you will find a "<x>.sln"
		 * file for a "<x>.sln.zip" that is contained in our dataset.
		 */
		Set<String> slnZips = IoHelper.findAllZips(ctxsDir);
		

		for (String slnZip : slnZips) {
			System.out.printf("\n#### processing solution zip: %s #####\n", slnZip);
			processSlnZip(slnZip);
		}
				
		editPrinter.flush();
		eventsPrinter.flush();
		allEventsPrinter.flush();
	}

	private void processSlnZip(String slnZip) {
		int numProcessedContexts = 0;

		// open the .zip file ...
		try (IReadingArchive ra = new ReadingArchive(new File(ctxsDir, slnZip))) {
			// ... and iterate over content.

			// the iteration will stop after 10 contexts to speed things up.
			while (ra.hasNext() && (numProcessedContexts++ < 10)) {
				/*
				 * within the slnZip, each stored context is contained as a single file that
				 * contains the Json representation of a Context.
				 */
				Context ctx = ra.getNext(Context.class);

				// the events can then be processed individually
				processContext(ctx);
			}
		}
	}

	private void processContext(Context ctx) {
		// a context is an abstract view on a single type declaration that contains of
		// two things:

		// 1) a simplified syntax tree of the type declaration
		process(ctx.getSST());

		// 2) a "type shape" that provides information about the hierarchy of the
		// declared type
		process(ctx.getTypeShape());
	}

	private void process(ITypeShape ts) {
		/*System.out.println("Start TypeShape");
		// a type shape contains hierarchy info for the declared type
		ITypeHierarchy th = ts.getTypeHierarchy();
		System.out.println("This is the hierarchy"+th);
		// the type that is being declared in the SST
		ITypeName tElem = th.getElement();
		// the type might extend another one (that again has a hierarchy)
		System.out.println("This is the tElem"+tElem);
		try {
			ITypeName tExt = th.getExtends().getElement();
			System.out.println("This is the tExt"+tExt);
		} catch (Exception e) {
			System.out.println("There was an exception...");
		}
		// or implement interfaces...
		for (ITypeHierarchy tImpl : th.getImplements()) {
			ITypeName tInterf = tImpl.getElement();
			System.out.println("Interfaces..."+tInterf);
		}

		// a type shape contains hierarchy info for all methods declared in the SST
		Set<IMemberHierarchy<IMethodName>> mhs = ts.getMethodHierarchies();
		for (IMemberHierarchy<IMethodName> mh : mhs) {
			// the declared element (you will find the same name in the SST)
			IMethodName elem = mh.getElement();
			System.out.println("the element is :"+ elem);

			// potentially, the method overrides another one higher in the hierarchy
			// (may be null)
			IMethodName sup = mh.getSuper();

			// in deep hierarchies, the method signature might have been introduced earlier
			// (may be null)
			IMethodName first = mh.getFirst();
			
			Set<IMemberHierarchy<IEventName>> ED= ts.getEventHierarchies(); 
			System.out.println("Heirarchy of Event NAME IS :" + ED);
		}

		// you can also access hierarchy information about other members...
		ts.getDelegates();*/
		ts.getEventHierarchies();
		
		Iterator<IMemberHierarchy<IEventName>> Ied =ts.getEventHierarchies().iterator();
		while (Ied.hasNext() ){
			IMemberHierarchy<IEventName> node =Ied.next();
			System.out.println("type shape from event declaration is"+node);
		Iterator<IMemberHierarchy<IMethodName>> Imh =ts.getMethodHierarchies().iterator();
		while(Imh.hasNext()){
			IMemberHierarchy<IMethodName> Imr =Imh.next();
			 System.out.println("Heirarchy of method name is "+Imr);
		}
		}
		
		ts.getFields();
		ts.getPropertyHierarchies();
		// ... and nested types
		ts.getNestedTypes();
		System.out.println("End TypeShape");
	
	}


	private void process(ISST sst) {
		System.out.println("Start SST");
		
		
	// SSTs represent a simplified meta model for source code. You can use the
		// various accessors to browse the contained information
      
		System.out.println("SST method started!!");
		// which type was edited?
		
		
		ITypeName declType = sst.getEnclosingType();
        
		// which methods are defined?
		for (IMethodDeclaration md : sst.getMethods()) {
			IMethodName m = md.getName();
			System.out.println("From SST,the method name is :" + m);
			for (IStatement stmt : md.getBody()) {
			
			
			Set<ITypeName> seenTypes = Sets.newHashSet();
			for (ITypeName type : seenTypes) {
				//// process the body...
			
				sst.accept(new CopyPasteVisitor(),seenTypes );
				
	
			}
		}
		// all references to types or type elements are fully qualified and preserve
		// many information about the resolved type
		declType.getNamespace();
		declType.isInterfaceType();
		declType.getAssembly();

		// you can distinguish reused types from types defined in a local project
		boolean isLocal = declType.getAssembly().isLocalProject();

		// the same is possible for all other <see>IName</see> subclasses, e.g.,
		// <see>IMethodName</see>
		
		m.getDeclaringType();
		m.getReturnType();
		// or inspect the signature
		for (IParameterName p : m.getParameters()) {
			String pid = p.getName();
			System.out.println("PID"+pid);
			ITypeName ptype = p.getValueType();
		}
		System.out.println("FYI SST ended...");
		
		System.out.println("End SST");
	}

	}
}
