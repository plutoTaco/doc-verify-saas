// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title DocumentRegistry
 * @notice Implements non-transferable (Soulbound) NFTs representing verified documents.
 * Stores document cryptographic SHA-256 hashes and IPFS Content Identifiers (CIDs).
 */
contract DocumentRegistry is ERC721, Ownable {
    uint256 private _nextTokenId;

    struct DocumentMetadata {
        bytes32 docHash; // SHA-256 hash of the PDF file
        string ipfsCid; // IPFS content identifier (off-chain storage path)
        uint256 issuedAt; // Timestamp of issuing
        address issuer; // Wallet address of issuing organization
    }

    // Mapping from Token ID to Document Metadata
    mapping(uint256 => DocumentMetadata) public documents;

    // Mapping from SHA-256 Hash to registration status (Prevents duplicate document registration)
    mapping(bytes32 => bool) public registeredHashes;

    // Events for off-chain indexing
    event DocumentIssued(
        uint256 indexed tokenId,
        bytes32 indexed docHash,
        string ipfsCid,
        address indexed issuer,
        address recipient
    );

    error DocumentAlreadyRegistered(bytes32 docHash);
    error InvalidDocumentHash();
    error SoulboundTokenNonTransferable();

    constructor(
        address initialOwner
    ) ERC721("Verified Document Token", "VDT") Ownable(initialOwner) {}

    /**
     * @notice Mints a non-transferable document NFT to a recipient.
     * @param recipient Address receiving the document proof.
     * @param docHash SHA-256 hash formatted as bytes32.
     * @param ipfsCid IPFS hash pointing to the stored PDF.
     */
    function issueDocument(
        address recipient,
        bytes32 docHash,
        string memory ipfsCid
    ) external onlyOwner returns (uint256) {
        if (docHash == bytes32(0)) revert InvalidDocumentHash();
        if (registeredHashes[docHash])
            revert DocumentAlreadyRegistered(docHash);

        uint256 tokenId = ++_nextTokenId;

        // Save metadata on-chain
        documents[tokenId] = DocumentMetadata({
            docHash: docHash,
            ipfsCid: ipfsCid,
            issuedAt: block.timestamp,
            issuer: msg.sender
        });

        registeredHashes[docHash] = true;

        // Mint ERC-721 token
        _safeMint(recipient, tokenId);

        emit DocumentIssued(tokenId, docHash, ipfsCid, msg.sender, recipient);

        return tokenId;
    }

    /**
     * @notice Enforces Soulbound behavior (Disallows transfers between non-zero addresses).
     */
    function _update(
        address to,
        uint256 tokenId,
        address auth
    ) internal override returns (address) {
        address from = _ownerOf(tokenId);

        // Allow minting (from == 0) and burning (to == 0), block transfers
        if (from != address(0) && to != address(0)) {
            revert SoulboundTokenNonTransferable();
        }

        return super._update(to, tokenId, auth);
    }

    /**
     * @notice Helper to check if a specific SHA-256 hash has been issued on-chain.
     */
    function verifyHash(bytes32 docHash) external view returns (bool) {
        return registeredHashes[docHash];
    }
}
