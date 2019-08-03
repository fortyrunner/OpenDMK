/*
 * @(#)file      PlainServer.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.4
 * @(#)date      07/10/01
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the
 * LEGAL_NOTICES folder that accompanied this code. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 *
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 *       "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 *
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 *
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 */

package com.sun.jdmk.security.sasl.plain;

import com.sun.jdmk.security.sasl.AuthenticateCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Implements the PLAIN SASL server mechanism.
 * (<A HREF="http://ftp.isi.edu/in-notes/rfc2595.txt">RFC 2595</A>)
 * <p>
 * Requires the following callbacks to be satisfied by the callback handler
 * when using PLAIN:
 * <ul>
 *   <li>AuthenticateCallback: to verify the authentication identity and
 *       password with the system authentication database.</li>
 *   <li>AuthorizeCallback: to verify that the authentication credentials
 *       permit the client to log in as the authorization identity.</li>
 * </ul>
 */
final class PlainServer implements SaslServer {

  public PlainServer(CallbackHandler cbh) {
    this.cbh = cbh;
  }

  @Override
  public String getMechanismName() {
    return "PLAIN";
  }

  @Override
  public byte[] evaluateResponse(byte[] response) throws SaslException {
    if (completed) {
      throw new IllegalStateException(
        "PLAIN authentication already completed");
    }
    completed = true;

    // Extract authorization_id/authentication_id/password from response
    //
    int indexSeparator1 = -1;
    int indexSeparator2 = -1;
    for (int i = 0; i < response.length; i++) {
      if (response[i] != SEPARATOR) {
      } else {
        if (indexSeparator1 == -1) {
          indexSeparator1 = i;
        } else {
          if (indexSeparator2 == -1) {
            indexSeparator2 = i;
          }
        }
      }
    }
    if ((indexSeparator1 < 0) ||
      (indexSeparator2 < 0) ||
      (indexSeparator1 + 1 == indexSeparator2) ||
      (indexSeparator2 + 1 == response.length)) {
      throw new IllegalStateException("PLAIN authentication error: " +
        "Response format should be: " +
        "[authorization_id]" +
        "<US-ASCII NUL>" +
        "authentication_id" +
        "<US-ASCII NUL>" +
        "password.");
    }
    int authzidSize = indexSeparator1;
    int authnidSize = indexSeparator2 - indexSeparator1 - 1;
    int passwdSize = response.length - indexSeparator2 - 1;
    byte[] authzid = new byte[authzidSize];
    byte[] authnid = new byte[authnidSize];
    byte[] passwd = new byte[passwdSize];
    System.arraycopy(response, 0, authzid, 0, authzidSize);
    System.arraycopy(response, indexSeparator1 + 1, authnid, 0, authnidSize);
    System.arraycopy(response, indexSeparator2 + 1, passwd, 0, passwdSize);
    String authenticationID;
    String authorizationID;
    String password;
    authenticationID = new String(authnid, StandardCharsets.UTF_8);
    password = new String(passwd, StandardCharsets.UTF_8);
    for (int i = 0; i < passwd.length; i++) {
      passwd[i] = 0;
    }
    passwd = null;
    if (authzid.length == 0) {
      authorizationID = authenticationID;
    } else {
      authorizationID = new String(authzid, StandardCharsets.UTF_8);
    }

    // Let the callback handler verify the remote authentication identity
    // and password with the system authentication database.
    //
    verifyAuthenticationCredentials(authenticationID, password);

    // Let the callback handler verify that the authentication credentials
    // permit the client to log in as the authorization identity.
    //
    verifyAuthorizationID(authenticationID, authorizationID);

    return null;
  }

  @Override
  public String getAuthorizationID() {
    return authorizationID;
  }

  @Override
  public boolean isComplete() {
    return completed;
  }

  @Override
  public byte[] unwrap(byte[] incoming, int offset, int len)
    throws SaslException {
    if (completed) {
      throw new SaslException(
        "PLAIN supports neither integrity nor privacy");
    } else {
      throw new IllegalStateException(
        "PLAIN authentication not completed");
    }
  }

  @Override
  public byte[] wrap(byte[] outgoing, int offset, int len)
    throws SaslException {
    if (completed) {
      throw new SaslException(
        "PLAIN supports neither integrity nor privacy");
    } else {
      throw new IllegalStateException(
        "PLAIN authentication not completed");
    }
  }

  @Override
  public Object getNegotiatedProperty(String propName) {
    if (completed) {
      if (propName.equals(Sasl.QOP)) {
        return "auth";
      } else {
        return null;
      }
    } else {
      throw new IllegalStateException(
        "PLAIN authentication not completed");
    }
  }

  @Override
  public void dispose() throws SaslException {
  }

  private void verifyAuthenticationCredentials(String username,
                                               String password)
    throws SaslException {
    final String msg =
      "PLAIN: Authentication credentials verification failed!";
    char[] passwd = password.toCharArray();
    AuthenticateCallback authnCb =
      new AuthenticateCallback(username, passwd);
    for (int i = 0; i < passwd.length; i++) {
      passwd[i] = ' ';
    }
    passwd = null;
    try {
      cbh.handle(new Callback[]{authnCb});
    } catch (IOException | UnsupportedCallbackException e) {
      throw new SaslException(msg, e);
    }
    authnCb.clearPassword();
    if (!authnCb.isAuthenticated()) {
      throw new AuthenticationException(msg);
    }
  }

  private void verifyAuthorizationID(String authenticationID,
                                     String authorizationID)
    throws SaslException {
    final String msg = "PLAIN: AuthorizationID verification failed!";
    AuthorizeCallback authzCb =
      new AuthorizeCallback(authenticationID, authorizationID);
    try {
      cbh.handle(new Callback[]{authzCb});
    } catch (IOException | UnsupportedCallbackException e) {
      throw new SaslException(msg, e);
    }
    if (authzCb.isAuthorized()) {
      this.authorizationID = authzCb.getAuthorizedID();
    } else {
      throw new SaslException("PLAIN: " +
        authenticationID +
        " is not authorized to act as " +
        authorizationID);
    }
  }

  private boolean completed;
  private final CallbackHandler cbh;
  private String authorizationID;
  private static final byte SEPARATOR = 0; // US-ASCII <NUL>
}
