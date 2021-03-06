package com.jetbrains.php.psalm.lang.documentation.parser;

import com.jetbrains.php.lang.parser.PhpParserTestCase;
import com.jetbrains.php.psalm.types.PsalmTypeInferenceTest;

public class PsalmDocParserTest extends PhpParserTestCase {

  public static final String DATA_PATH = PsalmTypeInferenceTest.TEST_DATA_HOME + "parser";

  @Override
  protected String getDataPath() {
    return DATA_PATH;
  }

  public void test$Param() throws Throwable {
    doTest();
  }

  public void test$Return() throws Throwable {
    doTest();
  }

  public void test$Var() throws Throwable {
    doTest();
  }

  public void test$ExtendedConstantsInPhpDocType() throws Throwable {
    doTest();
  }
}
