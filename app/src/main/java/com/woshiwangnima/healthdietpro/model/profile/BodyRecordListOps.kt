package com.woshiwangnima.healthdietpro.model.profile

internal fun List<BodyRecord>.removeRecordAt(index: Int): List<BodyRecord> =
    filterIndexed { currentIndex, _ -> currentIndex != index }
