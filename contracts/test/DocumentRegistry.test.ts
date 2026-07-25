import { expect } from "chai";
import hre from "hardhat";

describe("DocumentRegistry Smart Contract", function () {
  let documentRegistry: any;
  let owner: any;
  let student: any;
  let anotherUser: any;

  let dummyDocHash: string;
  const dummyIpfsCid = "QmTest1234567890";

  beforeEach(async function () {
    // THE HARDHAT 3 SECRET: Boot up a network simulation to get Ethers!
    const { ethers } = await hre.network.create();

    const signers = await ethers.getSigners();
    owner = signers[0];
    student = signers[1];
    anotherUser = signers[2];
    
    dummyDocHash = ethers.id("Sample PDF Content Hash"); 

    const DocumentRegistryFactory = await ethers.getContractFactory("DocumentRegistry");
    documentRegistry = await DocumentRegistryFactory.deploy(owner.address);
  });

  describe("Deployment", function () {
    it("Should set the right owner", async function () {
      expect(await documentRegistry.owner()).to.equal(owner.address);
    });

    it("Should have the correct name and symbol", async function () {
      expect(await documentRegistry.name()).to.equal("Verified Document Token");
      expect(await documentRegistry.symbol()).to.equal("VDT");
    });
  });

  describe("Issuing Documents", function () {
    it("Should allow the owner to issue a document NFT", async function () {
      await expect(documentRegistry.issueDocument(student.address, dummyDocHash, dummyIpfsCid))
        .to.emit(documentRegistry, "DocumentIssued")
        .withArgs(1, dummyDocHash, dummyIpfsCid, owner.address, student.address);

      expect(await documentRegistry.ownerOf(1)).to.equal(student.address);
    });

    it("Should save the correct document metadata on-chain", async function () {
      await documentRegistry.issueDocument(student.address, dummyDocHash, dummyIpfsCid);
      
      const docData = await documentRegistry.documents(1);
      
      expect(docData.docHash).to.equal(dummyDocHash);
      expect(docData.ipfsCid).to.equal(dummyIpfsCid);
      expect(docData.issuer).to.equal(owner.address);
    });

    it("Should reject non-owners from issuing documents", async function () {
      await expect(
        documentRegistry.connect(anotherUser).issueDocument(student.address, dummyDocHash, dummyIpfsCid)
      ).to.be.revertedWithCustomError(documentRegistry, "OwnableUnauthorizedAccount");
    });

    it("Should prevent issuing the exact same document hash twice", async function () {
      await documentRegistry.issueDocument(student.address, dummyDocHash, dummyIpfsCid);

      await expect(
        documentRegistry.issueDocument(anotherUser.address, dummyDocHash, "QmDifferentCID")
      ).to.be.revertedWithCustomError(documentRegistry, "DocumentAlreadyRegistered");
    });
  });

  describe("Soulbound Enforcement (Non-Transferable)", function () {
    it("Should prevent users from transferring their document NFTs", async function () {
      await documentRegistry.issueDocument(student.address, dummyDocHash, dummyIpfsCid);

      await expect(
        documentRegistry.connect(student).transferFrom(student.address, anotherUser.address, 1)
      ).to.be.revertedWithCustomError(documentRegistry, "SoulboundTokenNonTransferable");
    });
  });
});