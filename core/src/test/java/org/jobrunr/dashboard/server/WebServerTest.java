package org.jobrunr.dashboard.server;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebServerTest {

    @Test
    void httpHandlersAreClosedWhenWebserverIsStopped() {
        // GIVEN
        final HttpExchangeHandler httpExchangeHandlerMock = Mockito.mock(HttpExchangeHandler.class);
        when(httpExchangeHandlerMock.getContextPath()).thenReturn("/some-context-path");

        final WebServerHttp webServerHttp = new WebServerHttp(8000);
        webServerHttp.createContext(httpExchangeHandlerMock);

        // WHEN
        webServerHttp.stop();

        // THEN
        verify(httpExchangeHandlerMock).close();
    }

}