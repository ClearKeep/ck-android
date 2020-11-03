/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.clearkeep.chat.signal_store;

import android.text.TextUtils;

import com.clearkeep.utilities.storage.Storage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InMemoryPreKeyStore implements PreKeyStore {

  private static final String PARAM_PRE_KEY = "signal_pre_key";

  private final Storage storage;

  private Map<Integer, byte[]> store = new HashMap<>();

  public InMemoryPreKeyStore(Storage storage) {
    this.storage = storage;
    loadDataFromLocal();
  }

  @Override
  public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
    try {
      if (!store.containsKey(preKeyId)) {
        throw new InvalidKeyIdException("No such prekeyrecord!");
      }

      return new PreKeyRecord(store.get(preKeyId));
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void storePreKey(int preKeyId, PreKeyRecord record) {
    store.put(preKeyId, record.serialize());
    // save to local
    saveDataToLocal(store);
  }

  @Override
  public boolean containsPreKey(int preKeyId) {
    return store.containsKey(preKeyId);
  }

  @Override
  public void removePreKey(int preKeyId) {
    /*store.remove(preKeyId);*/
  }

  private void loadDataFromLocal() {
    String json= storage.getString(PARAM_PRE_KEY);
    if (!TextUtils.isEmpty(json)) {
      TypeToken<HashMap<Integer, byte[]>> token = new TypeToken<HashMap<Integer, byte[]>>() {};
      store = new Gson().fromJson(json, token.getType());
    }
  }

  private void saveDataToLocal(Map<Integer, byte[]> jsonMap) {
    String jsonString = new Gson().toJson(jsonMap);
    storage.setString(PARAM_PRE_KEY, jsonString);
  }
}
