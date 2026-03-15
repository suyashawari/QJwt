import secrets
import base64
import logging
from config import settings

try:
    from qiskit import QuantumCircuit
    from qiskit_ibm_runtime import QiskitRuntimeService, Sampler
    QISKIT_AVAILABLE = True
except ImportError as e:
    QISKIT_AVAILABLE = False
    _import_error = e

logger = logging.getLogger("QuantumSource")

class QuantumEntropySource:
    def __init__(self):
        self.service = None
        self.backend = None
        self._quantum_available = False
        self._init_quantum()

    def _init_quantum(self):
        """Attempt to initialize IBM Quantum connection."""
        if not QISKIT_AVAILABLE:
            logger.error(f"Qiskit not available: {_import_error}")
            return

        if not settings.ibm_quantum_token:
            logger.warning("IBM Quantum token not set. Quantum mode disabled.")
            return

        try:
            logger.info(f"Attempting to initialize QiskitRuntimeService with channel: ibm_cloud")
            # Try the "ibm_cloud" channel which uses the modern IBM Cloud endpoints
            self.service = QiskitRuntimeService(
                channel="ibm_cloud",
                token=settings.ibm_quantum_token
            )
            logger.info("QiskitRuntimeService object created. Fetching backend...")
            # Try to get the specified backend, fallback to least busy if not found
            try:
                self.backend = self.service.backend(settings.ibm_backend_name)
                logger.info(f"Successfully connected to backend: {settings.ibm_backend_name}")
            except Exception as backend_err:
                logger.warning(f"Backend '{settings.ibm_backend_name}' not found or unreachable: {backend_err}. Falling back to least busy.")
                self.backend = self.service.least_busy(operational=True, simulator=False)
            
            logger.info(f"🔮 IBM Quantum Service Initialized on backend: {self.backend.name}")
            self._quantum_available = True
        except Exception as e:
            logger.error(f"Failed to initialize IBM Quantum: {e}", exc_info=True)
            self._quantum_available = False

    def is_quantum_available(self):
        return self._quantum_available

    def _generate_classical_entropy(self, size_bytes: int) -> str:
        raw_bytes = secrets.token_bytes(size_bytes)
        return base64.urlsafe_b64encode(raw_bytes).decode('utf-8').rstrip('=')

    def get_classical_batch(self, count: int, size_bytes: int) -> list[str]:
        """Generate a batch of classical keys."""
        return [self._generate_classical_entropy(size_bytes) for _ in range(count)]

    def _generate_quantum_entropy(self, count: int) -> list[str]:
        if not self._quantum_available:
            raise Exception("IBM Quantum not available")

        # Workaround for UnboundLocalError in qiskit-ibm-runtime 0.20.0 with Heron backends
        try:
            num_qubits = min(127, self.backend.num_qubits)
        except Exception as e:
            logger.warning(f"Could not fetch num_qubits from backend, defaulting to 127: {e}")
            num_qubits = 127

        qc = QuantumCircuit(num_qubits)
        qc.h(range(num_qubits))
        qc.measure_all()

        logger.info("⚡ Submitting job to IBM Quantum Processor...")
        # Fix for qiskit-ibm-runtime 0.20.0 (V1 Primitives style)
        sampler = Sampler(backend=self.backend)
        job = sampler.run([qc], shots=count)
        result = job.result()
        data_pub = result[0].data.meas.get_bitstrings()

        keys = []
        for bitstring in data_pub:
            # Convert bitstring to bytes
            key_bytes = int(bitstring, 2).to_bytes((len(bitstring) + 7) // 8, byteorder='big')
            keys.append(base64.urlsafe_b64encode(key_bytes).decode().rstrip('='))
        return keys

    def get_entropy_batch(self, count: int, size_bytes: int) -> list[str]:
        """Get a batch of entropy, trying quantum first, falling back to classical."""
        if self._quantum_available:
            try:
                return self._generate_quantum_entropy(count)
            except Exception as e:
                logger.error(f"Quantum generation failed: {e}", exc_info=True)
                self._quantum_available = False
                # fall through to classical
        return self.get_classical_batch(count, size_bytes)