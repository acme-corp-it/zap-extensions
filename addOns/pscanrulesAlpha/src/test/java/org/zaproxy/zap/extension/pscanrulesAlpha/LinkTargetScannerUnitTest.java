/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2018 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.pscanrulesAlpha;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.parosproxy.paros.core.scanner.Plugin.AlertThreshold;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.Session;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.model.Context;
import org.zaproxy.zap.utils.ZapXmlConfiguration;

public class LinkTargetScannerUnitTest extends PassiveScannerTest<LinkTargetScanner> {

    private static final String HTML_CONTENT_TYPE = "text/html;charset=ISO-8859-1";
    private static final String TEXT_CONTENT_TYPE = "text/plain; charset=us-ascii";

    @Override
    protected LinkTargetScanner createScanner() {
        LinkTargetScanner scanner = new LinkTargetScanner();
        scanner.setConfig(new ZapXmlConfiguration());
        return scanner;
    }

    private String getHeader(String contentType, int bodyLength) {
        return "HTTP/1.1 200 OK\r\n"
                + "Content-Type: "
                + contentType
                + "\r\n"
                + "Content-Length: "
                + bodyLength
                + "\r\n";
    }

    @Test
    public void dontRaiseIssueWhenNoLinks() throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody("<html></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void dontRaiseIssueWhenOneLinkNoTarget() throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody("<html><a href=\"http://www.example.com\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void dontRaiseIssueWhenOneLinkWithBlankTargetSameDomain()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        Context context1 = new Context(session, 0);
        context1.addIncludeInContextRegex("https://www.example.com.*");
        context1.addIncludeInContextRegex("https://www.example2.com.*");
        List<Context> contextList = new ArrayList<Context>();
        contextList.add(context1);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(contextList);
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html><a href=\"https://www.example2.com/\" target=\"_blank\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void dontRaiseIssueWhenOneLinkWithBlankTargetTrustedDomain()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        rule.getConfig()
                .setProperty(
                        LinkTargetScanner.TRUSTED_DOMAINS_PROPERTY, "https://www.example2.com/.*");
        // When
        msg.setResponseBody(
                "<html><a href=\"https://www.example2.com/page1\" target=\"_blank\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void raiseIssueWhenOneLinkWithBlankTargetUntrustedDomain()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        rule.getConfig()
                .setProperty(
                        LinkTargetScanner.TRUSTED_DOMAINS_PROPERTY, "https://www.example2.com/.*");
        // When
        msg.setResponseBody(
                "<html><a href=\"https://www.example3.com/page1\" target=\"_blank\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<a href=\"https://www.example3.com/page1\" target=\"_blank\">link</a>"));
    }

    @Test
    public void raiseIssueWhenOneLinkWithBlankTargetDifferentDomain()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html><a href=\"http://www.example2.com\" target=\"_blank\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<a href=\"http://www.example2.com\" target=\"_blank\">link</a>"));
    }

    @Test
    public void raiseIssueWhenOneLinkWithOtherTarget() throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html><a href=\"http://www.example2.com\" target=\"other\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<a href=\"http://www.example2.com\" target=\"other\">link</a>"));
    }

    @Test
    public void dontRaiseIssueWhenOneLinkWithOtherTargetHighThreshold()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        rule.setAlertThreshold(AlertThreshold.HIGH);
        // When
        msg.setResponseBody(
                "<html><a href=\"http://www.example2.com\" target=\"other\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void raiseIssueWhenAreaWithBlankTargetDifferentDomain()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html> <img src=\"planets.gif\" width=\"145\" height=\"126\" alt=\"Planets\"  usemap=\"#planetmap\">"
                        + "<map name=\"planetmap\">"
                        + "  <area shape=\"rect\" coords=\"0,0,82,126\" href=\"https://www.example.com/sun.html\" target=\"_blank\" alt=\"Sun\">"
                        + "  <area shape=\"circle\" coords=\"90,58,3\" href=\"https://www.example.com2/mercur.html\" target=\"_blank\" alt=\"Mercury\">"
                        + "  <area shape=\"circle\" coords=\"124,58,8\" href=\"https://www.example.com/venus.html\" target=\"_blank\" alt=\"Venus\">"
                        + "</map> </html>");

        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo(
                        "<area shape=\"circle\" coords=\"90,58,3\" href=\"https://www.example.com2/mercur.html\" target=\"_blank\" alt=\"Mercury\">"));
    }

    @Test
    public void dontRaiseIssueWhenAreaWithOtherTarget() throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html> <img src=\"planets.gif\" width=\"145\" height=\"126\" alt=\"Planets\"  usemap=\"#planetmap\">"
                        + "<map name=\"planetmap\">"
                        + "  <area shape=\"rect\" coords=\"0,0,82,126\" href=\"https://www.example.com/sun.html\" target=\"_blank\" alt=\"Sun\">"
                        + "  <area shape=\"circle\" coords=\"90,58,3\" href=\"mercur.html\"  target=\"_blank\"alt=\"Mercury\">"
                        + "  <area shape=\"circle\" coords=\"124,58,8\" href=\"https://www.example.com/venus.html\" target=\"_blank\" alt=\"Venus\">"
                        + "</map> </html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void dontRaiseIssueWhenManyLinksWithBlankTargetSameDomain()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html>"
                        + "<a href=\"http://www.example.com\" target=\"_blank\">link</a>"
                        + "<a href=\"http://www.example.com\" target=\"_blank\">link</a>"
                        + "<a href=\"http://www.example.com\" target=\"_blank\">link</a>"
                        + "<a href=\"http://www.example.com\" target=\"_blank\">link</a>"
                        + "<a href=\"http://www.example.com\" target=\"_blank\">link</a>"
                        + "<a href=\"http://www.example.com\" target=\"_blank\">link</a>"
                        + "</html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void raiseIssueWhenManyLinksWithBlankTargetDifferentDomain()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html>"
                        + "<a href=\"http://www.example2.com\" target=\"_blank\">link</a>"
                        + "<a href=\"http://www.example2.com\" target=\"_blank\">link</a>"
                        + "<a href=\"http://www.example2.com\" target=\"_blank\">link</a>"
                        + "<a href=\"http://www.example2.com\" target=\"_blank\">link</a>"
                        + "<a href=\"http://www.example2.com\" target=\"_blank\">link</a>"
                        + "<a href=\"http://www.example2.com\" target=\"_blank\">link</a>"
                        + "</html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<a href=\"http://www.example2.com\" target=\"_blank\">link</a>"));
    }

    @Test
    public void dontRaiseIssueWhenOneLinkWithBlankTargetNoopenerNoReferrer()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html><a href=\"http://www.example2.com\" target=\"_blank\" rel=\"noopener noreferrer\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void dontRaiseIssueWhenOneLinkWithBlankTargetNoreferrerNoopener()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html><a href=\"http://www.example2.com\" target=\"_blank\" rel=\"noreferrer noopener\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void dontRaiseIssueWhenManyLinksWithOneBlankTargetSameDomain()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html>"
                        + "<a href=\"http://www.example.com/1\">link</a>"
                        + "<a href=\"http://www.example.com/2\">link</a>"
                        + "<a href=\"http://www.example.com/3\" target=\"other\">link</a>"
                        + "<a href=\"http://www.example.com/4\" target=\"_blank\" rel=\"noopener\">link</a>"
                        + "<a href=\"http://www.example.com/5\" target=\"_blank\" rel=\"noopener\">link</a>"
                        + "<a href=\"http://www.example.com/6\" target=\"_blank\">link</a>"
                        + "</html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void raiseIssueWhenManyLinksWithOneBlankTargetDifferentDomain()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html>"
                        + "<a href=\"http://www.example2.com/1\">link</a>"
                        + "<a href=\"http://www.example.com/2\">link</a>"
                        + "<a href=\"http://www.example2.com/3\">link</a>"
                        + "<a href=\"http://www.example2.com/4\" target=\"_blank\" rel=\"noopener noreferrer\">link</a>"
                        + "<a href=\"http://www.example2.com/5\" target=\"_blank\" rel=\"noreferrer noopener\">link</a>"
                        + "<a href=\"http://www.example2.com/6\" target=\"_blank\">link</a>"
                        + "</html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<a href=\"http://www.example2.com/6\" target=\"_blank\">link</a>"));
    }

    @Test
    public void dontRaiseIssueWhenOneLinkWithBlankTargetNonHtml()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(new ArrayList<Context>());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html><a href=\"http://www.example2.com\" target=\"_blank\">link</a></html>");
        msg.setResponseHeader(getHeader(TEXT_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void dontRaiseIssueWhenOneLinkWithBlankTargetInContext()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        Context context = new Context(session, 0);
        context.addIncludeInContextRegex("https://www.example.com/.*");
        context.addIncludeInContextRegex("https://www.example2.com/.*");
        ArrayList<Context> contexts = new ArrayList<Context>();
        contexts.add(context);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(contexts);
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html><a href=\"https://www.example2.com/\" target=\"_blank\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    public void raiseIssueWhenOneLinkWithBlankTargetNotInContext()
            throws HttpMalformedHeaderException {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        Context context = new Context(session, 0);
        context.addIncludeInContextRegex("https://www.example.com/.*");
        context.addIncludeInContextRegex("https://www.example2.com/.*");
        ArrayList<Context> contexts = new ArrayList<Context>();
        contexts.add(context);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(contexts);
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        // When
        msg.setResponseBody(
                "<html><a href=\"http://www.example3.com/\" target=\"_blank\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<a href=\"http://www.example3.com/\" target=\"_blank\">link</a>"));
    }

    @Test
    public void shouldNotFailIfLinkOrAreaDoesNotHaveHref() throws Exception {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        Context context = Mockito.mock(Context.class);
        when(context.isInContext(Matchers.anyString())).thenReturn(true);
        ArrayList<Context> contexts = new ArrayList<>();
        contexts.add(context);
        when(session.getContextsForUrl(Matchers.anyString())).thenReturn(contexts);
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        msg.setResponseBody("<html><a>link</a><area>area</area></html>");
        // When
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then = no exception.
    }

    @Test
    public void shouldNotFailIfInvalidRegex() throws Exception {
        // Mock the model and session
        Model model = Mockito.mock(Model.class);
        Session session = Mockito.mock(Session.class);
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        rule.getConfig().setProperty(LinkTargetScanner.TRUSTED_DOMAINS_PROPERTY, "[");
        // When
        msg.setResponseBody(
                "<html><a href=\"https://www.example2.com/page1\" target=\"_blank\">link</a></html>");
        msg.setResponseHeader(getHeader(HTML_CONTENT_TYPE, msg.getResponseBody().length()));
        rule.scanHttpResponseReceive(msg, -1, this.createSource(msg));
        // Then = no exception.
    }
}
