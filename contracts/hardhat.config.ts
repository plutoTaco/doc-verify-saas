import toolbox from "@nomicfoundation/hardhat-toolbox-mocha-ethers";

const config = {
  solidity: {
    version: "0.8.24",
    settings: {
      evmVersion: "cancun" // Keeps our mcopy command working
    }
  },
  plugins: [toolbox]
};

export default config;