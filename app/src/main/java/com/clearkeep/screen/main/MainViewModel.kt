package com.clearkeep.screen.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.clearkeep.screen.main.item.WorkSpaceEntity
import javax.inject.Inject

class MainViewModel @Inject constructor(): ViewModel() {
    var listWorkSpace= MutableLiveData<List<WorkSpaceEntity>>()
   private var listWorkSpaceDummy= arrayListOf<WorkSpaceEntity>()
    init {
        listWorkSpaceDummy.add(WorkSpaceEntity(1,"","CK"))
        listWorkSpaceDummy.add(WorkSpaceEntity(2,"","VMO"))
        listWorkSpaceDummy.add(WorkSpaceEntity(3,"","SUN"))
        listWorkSpaceDummy.add(WorkSpaceEntity(5,"","VIN"))
        listWorkSpace.postValue(listWorkSpaceDummy)

    }
}