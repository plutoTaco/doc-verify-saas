import { network } from "hardhat";

async function main() {
  // Hardhat 3 Secret: Explicitly connect to the network passed in the CLI!
  const { ethers } = await network.connect();
    
  // Grab the first account Hardhat provides us to use as the deployer
  const [deployer] = await ethers.getSigners();
  
  console.log("Deploying DocumentRegistry with account:", deployer.address);

  // Fetch the compiled contract
  const DocumentRegistry = await ethers.getContractFactory("DocumentRegistry");
  
  // Deploy it, passing the deployer's address as the initialOwner
  const documentRegistry = await DocumentRegistry.deploy(deployer.address);

  // Wait for the transaction to be mined (Hardhat 3 syntax)
  await documentRegistry.waitForDeployment();

  // Get the final deployed address
  const contractAddress = await documentRegistry.getAddress();
  
  console.log("✅ DocumentRegistry successfully deployed to:", contractAddress);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});