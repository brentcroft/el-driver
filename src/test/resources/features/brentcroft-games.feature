Feature: Brentcroft Games

  Scenario: Play Brentcroft Shithead

    Given browser "brentcroft"

    Then apply steps
    """
    brentcroft.openFromFile( 'src/test/resources/sites/brentcroft-site.xml' );
    brentcroft.home.shithead.click();

    brentcroft.whileDo(
      () -> !shithead.exists(),
      ( i ) -> c:delay(100),
      10,
      ( seconds ) -> c:raise( c:format( 'Shithead site not opened after %.2f seconds.', [ seconds ] ) )
    );

    c:println( 'Shithead site opened.' );

    brentcroft.shithead.newGameButton.click();

    c:println( 'New game.' );

    brentcroft.shithead.whileDo(
      () -> !stack.equalsText( 'Stack size: 0' ),
      ( i ) -> [
        stepButton.click(),
        c:println( c:format( 'Step: %s', [ i ] ) )
      ],
      100,
      ( seconds ) -> c:raise( c:format( 'Shithead game not finished after %.2f seconds.', [ seconds ] ) )
    );

    c:println( 'Game finished.' );
    """
