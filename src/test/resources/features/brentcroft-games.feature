Feature: Brentcroft Games

  Scenario:
    Given site "src/test/resources/sites/brentcroft-site.xml" is open

    Then apply steps
    """
    home.shithead.click();
    shithead.whileDo( '!shithead.exists()', 'c:delay(100)', 100 );

    shithead.newGameButton.click(); \
    shithead.whileDo( '!stack.equalsText( "Stack size: 0" )', 'stepButton.click()', 100 );

    shithead.eval( "stack.equalsText('Stack size: 0')" );
    """
