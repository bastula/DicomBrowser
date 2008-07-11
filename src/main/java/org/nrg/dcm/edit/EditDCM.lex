package org.nrg.dcm.edit;

import java_cup.runtime.*;

import static org.nrg.dcm.edit.EditDCMSym.*;

%%

%class EditDCMLex

%unicode
%line
%column

%public
%final
// %abstract

%cupsym EditDCMSym
%cup
%cupdebug

%init{
	labels.add("UID");
%init}

%yylexthrow{
	LexAnalysisException
%yylexthrow}

%{
	private final StringBuilder string = new StringBuilder();
	
	private Symbol sym(int type) {
		return sym(type, yytext());
	}

	private Symbol sym(int type, Object value) {
		return new Symbol(type, yyline, yycolumn, value);
	}

	private void error(final String message)
	throws LexAnalysisException {
		throw new LexAnalysisException(message, yyline, yycolumn);
	}
	
	private final Label.Table labels = new Label.Table();
	
	public final Label getLabel(final String s) {
		return labels.get(s);
	}
	
	class LexAnalysisException extends Exception {
		private final static long serialVersionUID = 1L;
		LexAnalysisException(final String msg, int line, int col) {
			this(msg, line, col, null);
		}
		LexAnalysisException(final String msg, final int line, final int col, final Throwable e) {
			super(msg + " at line " + line + ", column " + col, e);
		}
	}
%}

HexD = [0-9a-fA-F]

TagStart = "("
TagEnd = ")"
TagSep = ","

InputCharacter = [^\r\n]
LineTerminator = \r|\n|\r\n
Whitespace = [ \t\f]

Comment = "//" {InputCharacter}* {LineTerminator}

%state STRING_LITERAL

%%
<YYINITIAL> {
	/* literals */
	\"			{ string.setLength(0); yybegin(STRING_LITERAL); }
	
	{TagStart}{HexD}{HexD}{HexD}{HexD}{TagSep}{HexD}{HexD}{HexD}{HexD}{TagEnd}	{
		String tagText = yytext();
		try {
			final StringBuilder parseable = new StringBuilder("0x");
			parseable.append(tagText.substring(1,5));
			parseable.append(tagText.substring(6,10));
			return sym(TAG, Integer.decode(parseable.toString()));
		} catch (NumberFormatException e) {
			throw new LexAnalysisException("unable to parse tag", yyline, yycolumn, e);
		}
	}
	
	/* constraint */
	"="			{ return sym(EQUALS); }
	"~"			{ return sym(MATCHES); }
	":"			{ return sym(CONSTRAINS); }
	
	/* assignment */
	":="		{ return sym(ASSIGN); }
	"-"			{ return sym(DELETE); }
	","			{ return sym(ARGSEP); }
	
	/* other operations */
	"echo"		{ return sym(ECHO); }
	"new"		{ return sym(NEW); }
	
	/* label */
	[a-zA-Z][a-zA-Z0-9_]*		{
									final Label label = labels.get(yytext());
									if (null == label) {
										error("no label " + yytext() + " defined");
									} else {
										return sym(LABEL, label);
									}
								}
	
	\\{LineTerminator}			{ /* ignore */ }	/* line continuation */
	{LineTerminator}			{ return sym(STATEMENT_END); }
	
	{Comment}					{ return sym(STATEMENT_END); }	/* empty statement */
	{Whitespace}				{ /* ignore */ }
}

<STRING_LITERAL> {
  \"                {
  						yybegin(YYINITIAL); 
                     	return sym(STRING, string.toString());
                    }
  [^\n\r\"\\]+      { string.append( yytext() ); }
  \\\"              { string.append('\"'); }
  \\                { string.append('\\'); }
  [\n\r]			{ error("newline found inside string literal"); }
}

/* anything else is an error */
.|\n		{ error("bad text (" + yytext() + ")"); }