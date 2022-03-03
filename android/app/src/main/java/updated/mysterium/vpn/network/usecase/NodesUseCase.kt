package updated.mysterium.vpn.network.usecase

import mysterium.GetProposalsRequest
import network.mysterium.vpn.R
import updated.mysterium.vpn.core.DeferredNode
import updated.mysterium.vpn.core.NodeRepository
import updated.mysterium.vpn.database.dao.NodeDao
import updated.mysterium.vpn.database.entity.NodeEntity
import updated.mysterium.vpn.model.manual.connect.CountryInfo
import updated.mysterium.vpn.model.manual.connect.PriceLevel
import updated.mysterium.vpn.model.manual.connect.Proposal

class NodesUseCase(
    private val nodeRepository: NodeRepository,
    private val nodeDao: NodeDao
) {

    companion object {
        const val ALL_COUNTRY_CODE = "ALL_COUNTRY"
        private const val SERVICE_TYPE = "wireguard"
        private const val NAT_COMPATIBILITY = "auto"
    }

    fun initDeferredNode(deferredNode: DeferredNode) {
        nodeRepository.deferredNode = deferredNode
    }

    suspend fun getAllProposals(): List<Proposal> {
        return createProposalList(getAllNodes())
    }

    suspend fun getCountryInfoList(): List<CountryInfo> {
        val request = GetProposalsRequest().apply {
            this.refresh = true
            serviceType = SERVICE_TYPE
            natCompatibility = NAT_COMPATIBILITY
        }
        val countryInfoList = nodeRepository.countries(request)
        val totalCountryInfo = CountryInfo(
            countryFlagRes = R.drawable.icon_all_countries,
            countryCode = ALL_COUNTRY_CODE,
            countryName = "",
            proposalsNumber = countryInfoList.size,
            isSelected = true
        )
        return mutableListOf<CountryInfo>().apply {
            add(0, totalCountryInfo)
            addAll(countryInfoList)
        }
    }

    suspend fun getFavourites(proposals: List<Proposal>) = checkFavouriteRelevance(
        allAvailableNodes = proposals,
        favourites = createProposalList(nodeDao.getFavourites())
    )

    suspend fun addToFavourite(
        proposal: Proposal
    ) = nodeDao.addToFavourite(NodeEntity(proposal, true))

    suspend fun deleteFromFavourite(proposal: Proposal) {
        nodeDao.deleteFromFavourite(proposal.providerID + proposal.serviceType)
    }

    suspend fun isFavourite(nodeId: String): NodeEntity? = nodeDao.getById(nodeId)

    private suspend fun getAllNodes(): List<NodeEntity> {
        val proposalsRequest = GetProposalsRequest().apply {
            this.refresh = true
            serviceType = SERVICE_TYPE
            natCompatibility = NAT_COMPATIBILITY
        }
        return nodeRepository.proposals(proposalsRequest)
            .map { NodeEntity(it) }
    }

    private fun createProposalList(allNodesList: List<NodeEntity>) = parsePriceLevel(allNodesList)

    private fun checkFavouriteRelevance(
        allAvailableNodes: List<Proposal>,
        favourites: List<Proposal>
    ): List<Proposal> {
        favourites.forEach { favourite ->
            val nodeEntity = allAvailableNodes.find { node ->
                node.providerID == favourite.providerID
            }
            if (nodeEntity == null) {
                favourite.isAvailable = false
            }
        }
        return favourites
    }

    private fun parsePriceLevel(proposals: List<NodeEntity>): List<Proposal> {
        val parsedProposals = proposals.map {
            Proposal(it)
        }
        val sortedByPriceNodes = ArrayList(proposals)
        sortedByPriceNodes.sortedBy {
            it.pricePerByte
        }.forEachIndexed { index, node ->
            if (index < parsedProposals.size) {
                when {
                    index <= (sortedByPriceNodes.size * 0.33) -> {
                        parsedProposals.find {
                            it.providerID == node.providerID
                        }?.priceLevel = PriceLevel.LOW
                    }
                    index <= (sortedByPriceNodes.size * 0.66) -> {
                        parsedProposals.find {
                            it.providerID == node.providerID
                        }?.priceLevel = PriceLevel.MEDIUM
                    }
                    else -> {
                        parsedProposals.find {
                            it.providerID == node.providerID
                        }?.priceLevel = PriceLevel.HIGH
                    }
                }
            }
        }
        return parsedProposals
    }
}
