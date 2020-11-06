package com.clearkeep.chat.signal_store;

import com.clearkeep.utilities.storage.Storage;

import org.whispersystems.libsignal.groups.SenderKeyName;
import org.whispersystems.libsignal.groups.state.SenderKeyRecord;
import org.whispersystems.libsignal.groups.state.SenderKeyStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InMemorySenderKeyStore implements SenderKeyStore {

  private final Map<SenderKeyName, SenderKeyRecord> store = new HashMap<>();

  private final Storage storage;

  public InMemorySenderKeyStore(Storage storage) {
    this.storage = storage;
  }

  @Override
  public void storeSenderKey(SenderKeyName senderKeyName, SenderKeyRecord record) {
    store.put(senderKeyName, record);
  }

  @Override
  public SenderKeyRecord loadSenderKey(SenderKeyName senderKeyName) {
    try {
      SenderKeyRecord record = store.get(senderKeyName);

      if (record == null) {
        return new SenderKeyRecord();
      } else {
        return new SenderKeyRecord(record.serialize());
      }
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}
