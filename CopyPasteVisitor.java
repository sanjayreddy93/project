package examples;

import java.util.Set;

import cc.kave.commons.model.naming.codeelements.IEventName;
import cc.kave.commons.model.naming.codeelements.IFieldName;
import cc.kave.commons.model.naming.codeelements.IMethodName;
import cc.kave.commons.model.naming.codeelements.IParameterName;
import cc.kave.commons.model.naming.types.ITypeName;
import cc.kave.commons.model.ssts.declarations.IEventDeclaration;
import cc.kave.commons.model.ssts.declarations.IFieldDeclaration;
import cc.kave.commons.model.ssts.declarations.IMethodDeclaration;
import cc.kave.commons.model.ssts.impl.visitor.AbstractTraversingNodeVisitor;
import cc.kave.commons.model.ssts.statements.IVariableDeclaration;

public class CopyPasteVisitor extends AbstractTraversingNodeVisitor<Set<ITypeName>, Void> {

	@Override
	public Void visit(IVariableDeclaration stmt, Set<ITypeName> seenTypes) {
		// for a variable declaration you can access the referenced type
		ITypeName type = stmt.getType();
		// let's put it into the index
		seenTypes.add(type);
		return null;
	}

	@Override
	public Void visit(IMethodDeclaration decl, Set<ITypeName> seenTypes) {
		// access the fully-qualified name of this method
		IMethodName name = decl.getName();

		// store the return type
		seenTypes.add(name.getReturnType());

		// iterate over all parameters of this method
		for (IParameterName param : name.getParameters()) {
			// and store the simple names
			seenTypes.add(param.getValueType());
		}

		// now continue with the traversal, implemented in the base class
		return super.visit(decl, seenTypes);
	}

	// ... there are many other visit methods that you should override to handle
	// different code elements, but I guess, you get the idea now. :	

	@Override
	public Void visit(IEventDeclaration Edecl, Set<ITypeName> seenTypes) {
		// access the fully-qualified name of this method
		IEventName name = Edecl.getName();
       
		// store the return type
		seenTypes.add(name.getHandlerType());
		// now continue with the traversal, implemented in the base class
		return super.visit(Edecl, seenTypes);
	}
	
	
	@Override
	public Void visit(IFieldDeclaration fdecl,Set<ITypeName> seenTypes) {
		IFieldName name = fdecl.getName();
		
		seenTypes.add(name.getValueType());
		
		
		return super.visit(fdecl, seenTypes);
	}
	
}

