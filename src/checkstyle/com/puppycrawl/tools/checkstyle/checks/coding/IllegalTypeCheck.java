////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2004  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle.checks.coding;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.AbstractFormatCheck;
import com.puppycrawl.tools.checkstyle.checks.CheckUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * Checks that particular class are never used as types in variable
 * declarations, return values or parameters. Includes
 * a pattern check that by default disallows abstract classes.
 * </p>
 * <p>
 * Rationale:
 * Helps reduce coupling on concrete classes. In addition abstract
 * classes should be thought of a convenience base class
 * implementations of interfaces and as such are not types themsleves.
 * </p>
 * @author <a href="mailto:simon@redhillconsulting.com.au">Simon Harris</a>
 */
public final class IllegalTypeCheck extends AbstractFormatCheck
{
    /** Default value of pattern for illegal class name. */
    private static final String DEFAULT_FORMAT = "^(.*[\\.])?Abstract.*$";
    /** Types illegal by default. */
    private static final String[] DEFAULT_ILLEGAL_TYPES = {
        "GregorianCalendar",
        "Hashtable",
        "HashSet",
        "HashMap",
        "ArrayList",
        "LinkedList",
        "LinkedHashMap",
        "LinkedHashSet",
        "TreeSet",
        "TreeMap",
        "Vector",
        "java.util.GregorianCalendar",
        "java.util.Hashtable",
        "java.util.HashSet",
        "java.util.HashMap",
        "java.util.ArrayList",
        "java.util.LinkedList",
        "java.util.LinkedHashMap",
        "java.util.LinkedHashSet",
        "java.util.TreeSet",
        "java.util.TreeMap",
        "java.util.Vector",
    };

    /** illegal classes. */
    private final Set mIllegalClassNames = new HashSet();
    /** methods which should be ignored. */
    private final Set mIgnoredMethodNames = new HashSet();

    /** Creates new instance of the check. */
    public IllegalTypeCheck()
    {
        super(DEFAULT_FORMAT);
        setIllegalClassNames(DEFAULT_ILLEGAL_TYPES);
        setIgnoredMethodNames(new String[] {
            "getInitialContext",
            "getEnvironment",
        });
    }

    /** {@inheritDoc} */
    public int[] getDefaultTokens()
    {
        return new int[] {
            TokenTypes.VARIABLE_DEF,
            TokenTypes.PARAMETER_DEF,
            TokenTypes.METHOD_DEF,
        };
    }

    /** {@inheritDoc} */
    public void visitToken(DetailAST aAST)
    {
        switch (aAST.getType()) {
        case TokenTypes.METHOD_DEF:
            visitMethodDef(aAST);
            break;
        case TokenTypes.VARIABLE_DEF:
            visitVariableDef(aAST);
            break;
        case TokenTypes.PARAMETER_DEF:
            visitParameterDef(aAST);
            break;
        default:
            throw new IllegalStateException(aAST.toString());
        }
    }

    /**
     * Checks return type of a given method.
     * @param aAST method for check.
     */
    private void visitMethodDef(DetailAST aAST)
    {
        if (isCheckedMethod(aAST)) {
            checkClassName(aAST);
        }
    }

    /**
     * Checks type of parameters.
     * @param aAST parameter list for check.
     */
    private void visitParameterDef(DetailAST aAST)
    {
        final DetailAST grandParentAST = aAST.getParent().getParent();

        if (grandParentAST.getType() == TokenTypes.METHOD_DEF
            && isCheckedMethod(grandParentAST))
        {
            checkClassName(aAST);
        }
    }

    /**
     * Checks type of given variable.
     * @param aAST variable to check.
     */
    private void visitVariableDef(DetailAST aAST)
    {
        checkClassName(aAST);
    }

    /**
     * Checks type of given method, parameter or variable.
     * @param aAST node to check.
     */
    private void checkClassName(DetailAST aAST)
    {
        final DetailAST type = aAST.findFirstToken(TokenTypes.TYPE);
        final FullIdent ident = CheckUtils.createFullType(type);

        if (isMatchingClassName(ident.getText())) {
            log(ident.getLineNo(), ident.getColumnNo(),
                "illegal.type", ident.getText());
        }
    }

    /**
     * @param aClassName class name to check.
     * @return true if given class name is one of illegal classes
     *         or if it matches to abstract class names pattern.
     */
    private boolean isMatchingClassName(String aClassName)
    {
        return mIllegalClassNames.contains(aClassName)
            || getRegexp().match(aClassName);
    }

    /**
     * @param aAST method def to check.
     * @return true if we should check this method.
     */
    private boolean isCheckedMethod(DetailAST aAST)
    {
        final String methodName =
            aAST.findFirstToken(TokenTypes.IDENT).getText();
        return !mIgnoredMethodNames.contains(methodName);
    }

    /**
     * Set the list of illegal variable types.
     * @param aClassNames array of illegal variable types
     */
    public void setIllegalClassNames(String[] aClassNames)
    {
        mIllegalClassNames.clear();
        for (int i = 0; i < aClassNames.length; i++) {
            final String name = aClassNames[i];
            mIllegalClassNames.add(name);
            final int lastDot = name.lastIndexOf(".");
            if (lastDot > 0 && lastDot < (name.length() - 1)) {
                final String shortName =
                    name.substring(name.lastIndexOf(".") + 1);
                mIllegalClassNames.add(shortName);
            }
        }
    }

    /**
     * Set the list of illegal variable types.
     * @return array of illegal variable types
     */
    public String[] getIllegalClassNames()
    {
        return (String[]) mIllegalClassNames.toArray(new String[0]);
    }

    /**
     * Set the list of ignore method names.
     * @param aMethodNames array of ignored method names
     */
    public void setIgnoredMethodNames(String[] aMethodNames)
    {
        mIgnoredMethodNames.clear();
        for (int i = 0; i < aMethodNames.length; i++) {
            mIgnoredMethodNames.add(aMethodNames[i]);
        }
    }

    /**
     * Set the list of ignored method names.
     * @return array of ignored method names
     */
    public String[] getIgnoredMethodNames()
    {
        return (String[]) mIgnoredMethodNames.toArray(new String[0]);
    }
}
