/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.screen.chat.signal_store;

import android.text.TextUtils;

import com.clearkeep.utilities.storage.Storage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InMemorySignedPreKeyStore implements SignedPreKeyStore {

  private static final String PARAM_SIGNED_KEY = "signal_signed_pre_key";

  private final Storage storage;

  private Map<Integer, byte[]> store = new HashMap<>();

  public InMemorySignedPreKeyStore(Storage storage) {
    this.storage = storage;
    loadDataFromLocal();
  }

  @Override
  public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
    try {
      if (!store.containsKey(signedPreKeyId)) {
        throw new InvalidKeyIdException("No such signedprekeyrecord! " + signedPreKeyId);
      }

      return new SignedPreKeyRecord(store.get(signedPreKeyId));
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public List<SignedPreKeyRecord> loadSignedPreKeys() {
    try {
      List<SignedPreKeyRecord> results = new LinkedList<>();

      for (byte[] serialized : store.values()) {
        results.add(new SignedPreKeyRecord(serialized));
      }

      return results;
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
    store.put(signedPreKeyId, record.serialize());

    saveDataToLocal(store);
  }

  @Override
  public boolean containsSignedPreKey(int signedPreKeyId) {
    return store.containsKey(signedPreKeyId);
  }

  @Override
  public void removeSignedPreKey(int signedPreKeyId) {
    /*store.remove(signedPreKeyId);*/
  }

  private void loadDataFromLocal() {
    String json= storage.getString(PARAM_SIGNED_KEY);
    if (!TextUtils.isEmpty(json)) {
      TypeToken<HashMap<Integer, byte[]>> token = new TypeToken<HashMap<Integer, byte[]>>() {};
      store = new Gson().fromJson(json, token.getType());
    }
  }

  private void saveDataToLocal(Map<Integer, byte[]> jsonMap) {
    String jsonString = new Gson().toJson(jsonMap);
    storage.setString(PARAM_SIGNED_KEY, jsonString);
  }
}
