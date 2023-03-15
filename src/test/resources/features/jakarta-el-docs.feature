Feature: Test Frames
  Scenario: Jakarta EL Docs
    Given browser "jakarta"
    Then apply steps
    """
    jakarta.openFromFile( 'src/test/resources/sites/jakarta-el.xml' );
    jakarta.classes.ifThenElse(
      () -> cookiesPopup.exists(),
      () -> [
        cookiesPopup.decline.click(),
        cookiesPopup.close.click()
      ],
      () -> c:inspect( jakarta, '' )
    );
    """
    Then apply steps
    """
    jakarta.packages.javaxScript.assertExists();
    jakarta.packages.javaxScript.click();
    """
    Then apply steps
    """
    jakarta.package.scriptEngineManager.assertExists();
    jakarta.package.scriptEngineManager.click();
    """
    Then apply steps
    """
    jakarta.classes.assertExists();
    jakarta.classes.scriptEngineManager.assertExists();
    """
