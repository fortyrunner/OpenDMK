/*
 * @(#)file      SASLMessage.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.10
 * @(#)lastedit  07/03/08
 * @(#)build     @BUILD_TAG_PLACEHOLDER@
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


package javax.management.remote.message;

/**
 * <p>A challenge or response exchanged between client and server
 * during SASL authentication.  This message encapsulates either a
 * challenge or a response generated by the SASL mechanism during the
 * SASL authentication exchanges taking place between the client and
 * the server.</p>
 *
 * <p>
 * The challenges/responses (blobs) are generated by the SASL mechanism:
 * <ul>
 *     <li>The <b>challenges</b> are generated by the server side SASL
 *         mechanisms.</li>
 *     <li>The <b>responses</b> are generated by the client side SASL
 *         mechanisms in response to the server challenges.</li>
 * </ul>
 * The status attribute takes one of the two values:
 * <ul>
 *     <li><b>CONTINUE</b>: used by either a client or server to indicate that
 *         they require more interaction with the other peer in order to
 *         complete the authentication exchange.</li>
 *     <li><b>COMPLETE</b>: used by a server to indicate that the exchange is
 *         complete and successful.</li>
 * </ul>
 * At any time during the SASL handshake, if the server encounters a problem it
 * can notify the client by sending an {@link HandshakeErrorMessage indication}
 * as to why the operation failed.
 * <p>
 * At any time during the SASL handshake, if the client encounters a problem or
 * wants to abort the authentication exchange it can notify the server by
 * sending an {@link HandshakeErrorMessage indication} as to why the operation
 * failed or is aborted.
 * <p>
 * The profile name in this profile message is built by concatenating the
 * prefix "SASL/" with the SASL mechanism name provided by the IANA SASL
 * registry.
 * <p>
 * Examples of SASL profile names are:
 * <ul>
 *     <li>SASL/GSSAPI</li>
 *     <li>SASL/EXTERNAL</li>
 *     <li>SASL/CRAM-MD5</li>
 *     <li>SASL/ANONYMOUS</li>
 *     <li>SASL/OTP</li>
 *     <li>SASL/PLAIN</li>
 *     <li>SASL/DIGEST-MD5</li>
 * </ul>
 *
 * @see HandshakeBeginMessage
 */
public class SASLMessage implements ProfileMessage {

  private static final long serialVersionUID = 429225478070724773L;

  /**
   * @serial The SASL mechanism.
   * @see #getMechanism()
   **/
  private String mechanism;

  /**
   * @serial The status of the current SASL authentication exchanges.
   * @see #getStatus()
   **/
  private int status;

  /**
   * @serial The blob generated by the SASL mechanism.
   * @see #getBlob()
   **/
  private byte[] blob;

  /**
   * This status code is used by either a client or server to indicate that
   * they require more interaction with the other peer in order to complete
   * the authentication exchange.
   */
  public static final int CONTINUE = 1;

  /**
   * This status code is used by a server to indicate that the authentication
   * exchange is complete and successful.
   */
  public static final int COMPLETE = 2;

  /**
   * Constructs a new SASLMessage with the specified SASL mechanism,
   * status and generated blob.
   *
   * @param mechanism the SASL mechanism
   * @param status    the status of the current SASL authentication
   *                  exchanges.
   * @param blob      the blob generated by the SASL mechanism.
   */
  public SASLMessage(String mechanism, int status, byte[] blob) {
    this.mechanism = mechanism;
    this.status = status;
    this.blob = blob;
  }

  /**
   * The SASL mechanism.
   *
   * @return the SASL mechanism.
   */
  public String getMechanism() {
    return mechanism;
  }

  /**
   * The status of the current SASL authentication exchanges.
   *
   * @return the status of the current SASL authentication exchanges.
   * @see #CONTINUE
   * @see #COMPLETE
   */
  public int getStatus() {
    return status;
  }

  /**
   * The blob generated by the SASL mechanism.
   *
   * @return the blob generated by the SASL mechanism.
   */
  public byte[] getBlob() {
    return blob;
  }

  /**
   * The profile name.
   */
  public String getProfileName() {
    return "SASL/" + mechanism;
  }
}
