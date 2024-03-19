package com.example.rv

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.ar.core.Config
import com.example.rv.ui.theme.RVTheme
import com.example.rv.ui.theme.Translucent
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.PlacementMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RVTheme {
                val navController = rememberNavController()
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    NavHost(navController = navController, startDestination = "homeScreen") {
                        composable("homeScreen") {
                            HomeScreen(navController = navController)
                        }
                        composable("detectionScreen") {
                            DetectionScreen()
                        }
                        composable("arScreen") {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val currentModel = remember {
                                    mutableStateOf("sucrose")
                                }

                                ARScreen(currentModel.value)

                                Menu(modifier = Modifier.align(Alignment.BottomCenter)) {
                                    currentModel.value = it
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.agua),
                contentDescription = "Agua",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bem-vindo(a) ao Chemistry AR",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("detectionScreen") },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Visualizar elementos em RA")
            }
        }
    }
}

@Composable
fun Menu(modifier: Modifier, onClick: (String) -> Unit) {
    var currentIndex by remember {
        mutableIntStateOf(0)
    }

    var showInfo by remember {
        mutableStateOf(false)
    }

    val itemsList = listOf(
        Molecule("glicose", R.drawable.glicose, "C12H22O11", 342.3f, "Disaccharide", listOf("Sweetening agent")),
        Molecule("acido-sulfurico", R.drawable.acido_sulfurico, "H2SO4", 98.08f, "Acid", listOf("Corrosive")),
        Molecule("agua", R.drawable.agua, "H2O", 18.01f, "Oxide", listOf("Solvent")),
        Molecule("benzeno", R.drawable.benzeno, "C6H6", 78.11f, "Hydrocarbon", listOf("Flammable", "Irritant", "Health Hazar", "Fuel")),
        Molecule("dioxido-carbono", R.drawable.dioxido_carbono, "CO2", 44.00f, "Oxide", listOf("Compressed Gas")),
    )

    fun updateIndex(offset: Int) {
        currentIndex = (currentIndex + offset + itemsList.size) % itemsList.size
        onClick(itemsList[currentIndex].name)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(onClick = { updateIndex(-1) }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_arrow_back_ios_24),
                    contentDescription = "previous"
                )
            }

            CircularImage(
                imageId = itemsList[currentIndex].imageId,
                imageName = itemsList[currentIndex].name,
                onClick = { showInfo = true }
            )

            IconButton(onClick = { updateIndex(1) }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_arrow_forward_ios_24),
                    contentDescription = "next"
                )
            }
        }

        if (showInfo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { showInfo = false }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_close_24),
                            contentDescription = "close"
                        )
                    }
                }

                Column {
                    Text("Name: ${itemsList[currentIndex].name}")
                    Text("Chemical Formula: ${itemsList[currentIndex].chemicalFormula}")
                    Text("Molecular Weight: ${itemsList[currentIndex].molecularWeight} g/mol")
                    Text("Type: ${itemsList[currentIndex].type}")
                    Text("Uses:")
                    itemsList[currentIndex].uses.forEach { use ->
                        Text(" - $use")
                    }

                    Exercise(molecule = itemsList[currentIndex])
                }
            }
        }
    }
}

@Composable
fun Exercise(molecule: Molecule) {
    val question = "Which of the following is a valid use of the molecule?"
    val answers = listOf(
        "Sweetening agent",
        "Solvent",
        "Fuel",
        "Antiseptic",
        "Detergent"
    )

    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = question,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        answers.forEach { answer ->
            Row(
                modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                    selectedAnswer = answer
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedAnswer == answer, onClick = { selectedAnswer = answer })
                Text(text = answer, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Button(
            onClick = {
                isCorrect = selectedAnswer in molecule.uses
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Submit")
        }

        if (selectedAnswer != null && isCorrect != null) {
            val color = if (isCorrect == true) Color.Green else Color.Red
            Text(
                if (isCorrect == true) "Correct!" else "Incorrect :/",
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun CircularImage(
    modifier: Modifier = Modifier,
    imageId: Int,
    imageName: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(140.dp)
            .clip(CircleShape)
            .border(width = 3.dp, Translucent, CircleShape)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = imageId),
            contentDescription = null,
            modifier = Modifier.size(140.dp),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
fun ARScreen(model: String) {
    val nodeList = remember {
        mutableListOf<ArNode>()
    }

    val modelNode = remember {
        mutableStateOf<ArModelNode?>(null)
    }

    val placeModelButton = remember {
        mutableStateOf(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            nodes = nodeList,
            planeRenderer = true,
            onCreate = { arSceneView ->
                arSceneView.lightEstimationMode = Config.LightEstimationMode.DISABLED
                arSceneView.planeRenderer.isShadowReceiver = false
                modelNode.value = ArModelNode(arSceneView.engine, PlacementMode.INSTANT).apply {
                    loadModelGlbAsync(
                        glbFileLocation = "models/${model}.glb",
                        scaleToUnits = 0.8f
                    ) {

                    }

                    onAnchorChanged = {
                        placeModelButton.value = !isAnchored
                    }

                    onHitResult = { node, hitResult ->
                        placeModelButton.value = node.isTracking
                    }
                }
                nodeList.add(modelNode.value!!)
            },
            onSessionCreate = {
                planeRenderer.isVisible = false
            }
        )

        if (placeModelButton.value) {
            Button(
                onClick = { modelNode.value?.anchor() },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(text = "Place It")
            }
        }
    }

    LaunchedEffect(key1 = model) {
        modelNode.value?.loadModelGlbAsync(
            glbFileLocation = "models/${model}.glb",
            scaleToUnits = 0.8f
        )

        Log.e("errorloading", "ERROR LOADING MODEL")
    }
}

@Composable
fun DetectionScreen() {
    //Implementar tela

}
data class Molecule(
    var name: String,
    var imageId: Int,
    var chemicalFormula: String,
    var molecularWeight: Float,
    var type: String,
    var uses: List<String>,
)