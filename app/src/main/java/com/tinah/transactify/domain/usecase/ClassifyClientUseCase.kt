package com.tinah.transactify.domain.usecase

import com.tinah.transactify.domain.model.ClientClassification
import com.tinah.transactify.utils.Constants

/**
 * Détermine la segmentation d'un client à partir de son activité.
 *
 * Règles :
 * - **VIP** : au moins [Constants.VIP_MIN_TRANSACTIONS] transactions **ou** un
 *   volume cumulé ≥ [Constants.VIP_MIN_VOLUME].
 * - **ONE_TIME** : au plus [Constants.ONE_TIME_MAX_TRANSACTIONS] transaction.
 * - **REGULAR** : tous les autres.
 */
class ClassifyClientUseCase {

    operator fun invoke(transactionCount: Int, totalVolume: Double): ClientClassification = when {
        transactionCount >= Constants.VIP_MIN_TRANSACTIONS ||
            totalVolume >= Constants.VIP_MIN_VOLUME -> ClientClassification.VIP

        transactionCount <= Constants.ONE_TIME_MAX_TRANSACTIONS -> ClientClassification.ONE_TIME

        else -> ClientClassification.REGULAR
    }
}
