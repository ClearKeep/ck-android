/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.screen.chat.signal_store;

import android.text.TextUtils;
import android.util.Base64;

import com.clearkeep.utilities.storage.Storage;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;

import java.util.List;

public class InMemorySignalProtocolStore implements SignalProtocolStore {

  private static final String IDENTITY_KEY = "signal_identity_key";

  private static final String REGISTRATION_KEY = "signal_registration_id";

  private final InMemoryPreKeyStore preKeyStore;

  private final InMemorySessionStore sessionStore;

  private final InMemorySignedPreKeyStore signedPreKeyStore;

  private final InMemoryIdentityKeyStore identityKeyStore;

  public InMemorySignalProtocolStore(Storage storage) throws InvalidKeyException {
    String identityKeyPairStr = storage.getString(IDENTITY_KEY);
    IdentityKeyPair identityKeyPair;
    if (!TextUtils.isEmpty(identityKeyPairStr)) {
      byte[] array = Base64.decode(identityKeyPairStr, Base64.DEFAULT);
      identityKeyPair = new IdentityKeyPair(array);
    } else {
      identityKeyPair = KeyHelper.generateIdentityKeyPair();
      String saveThis = Base64.encodeToString(identityKeyPair.serialize(), Base64.DEFAULT);
      storage.setString(IDENTITY_KEY, saveThis);
    }

    int registrationID = storage.getInt(REGISTRATION_KEY);
    if (registrationID == -1) {
      registrationID = KeyHelper.generateRegistrationId(false);
      storage.setInt(REGISTRATION_KEY, registrationID);
    }

    this.identityKeyStore = new InMemoryIdentityKeyStore(identityKeyPair, registrationID);
    this.preKeyStore = new InMemoryPreKeyStore(storage);
    this.sessionStore = new InMemorySessionStore();
    this.signedPreKeyStore = new InMemorySignedPreKeyStore(storage);
  }

  @Override
  public IdentityKeyPair getIdentityKeyPair() {
    return identityKeyStore.getIdentityKeyPair();
  }

  @Override
  public int getLocalRegistrationId() {
    return identityKeyStore.getLocalRegistrationId();
  }

  @Override
  public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
    return identityKeyStore.saveIdentity(address, identityKey);
  }

  @Override
  public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey, Direction direction) {
    return identityKeyStore.isTrustedIdentity(address, identityKey, direction);
  }

  @Override
  public IdentityKey getIdentity(SignalProtocolAddress address) {
    return identityKeyStore.getIdentity(address);
  }

  @Override
  public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
    return preKeyStore.loadPreKey(preKeyId);
  }

  @Override
  public void storePreKey(int preKeyId, PreKeyRecord record) {
    preKeyStore.storePreKey(preKeyId, record);
  }

  @Override
  public boolean containsPreKey(int preKeyId) {
    return preKeyStore.containsPreKey(preKeyId);
  }

  @Override
  public void removePreKey(int preKeyId) {
    preKeyStore.removePreKey(preKeyId);
  }

  @Override
  public SessionRecord loadSession(SignalProtocolAddress address) {
    return sessionStore.loadSession(address);
  }

  @Override
  public List<Integer> getSubDeviceSessions(String name) {
    return sessionStore.getSubDeviceSessions(name);
  }

  @Override
  public void storeSession(SignalProtocolAddress address, SessionRecord record) {
    sessionStore.storeSession(address, record);
  }

  @Override
  public boolean containsSession(SignalProtocolAddress address) {
    return sessionStore.containsSession(address);
  }

  @Override
  public void deleteSession(SignalProtocolAddress address) {
    sessionStore.deleteSession(address);
  }

  @Override
  public void deleteAllSessions(String name) {
    sessionStore.deleteAllSessions(name);
  }

  @Override
  public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
    return signedPreKeyStore.loadSignedPreKey(signedPreKeyId);
  }

  @Override
  public List<SignedPreKeyRecord> loadSignedPreKeys() {
    return signedPreKeyStore.loadSignedPreKeys();
  }

  @Override
  public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
    signedPreKeyStore.storeSignedPreKey(signedPreKeyId, record);
  }

  @Override
  public boolean containsSignedPreKey(int signedPreKeyId) {
    return signedPreKeyStore.containsSignedPreKey(signedPreKeyId);
  }

  @Override
  public void removeSignedPreKey(int signedPreKeyId) {
    signedPreKeyStore.removeSignedPreKey(signedPreKeyId);
  }
}
